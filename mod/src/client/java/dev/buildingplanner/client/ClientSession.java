package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.BoxCorners;
import dev.buildingplanner.input.BoxPurpose;
import dev.buildingplanner.input.CopyColour;
import dev.buildingplanner.input.CopyMode;
import dev.buildingplanner.input.CopyPlacement;
import dev.buildingplanner.input.CopyGrid;
import dev.buildingplanner.input.EraseHold;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.input.PasteBoxes;
import dev.buildingplanner.input.RealPlace;
import dev.buildingplanner.input.ShapeLock;
import dev.buildingplanner.layer.BoxCount;
import dev.buildingplanner.layer.DeleteMarks;
import dev.buildingplanner.layer.GhostErase;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
import dev.buildingplanner.look.ControlsLine;
import dev.buildingplanner.look.GhostsShown;
import dev.buildingplanner.sync.CopyRules;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.LayerReplica;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.malilib.gui.GuiBase;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Building Planner on one connection to a server: the player's copy of the layer, kept equal to the server's in the
 * columns the game has loaded, drawn through Litematica's tiles. The server is one that runs Building Planner, over the
 * network, or the player's own {@link LocalSession} on one that doesn't; {@link SessionLink} says which. Everything
 * runs on the client's main thread, where Fabric runs the payload handlers.
 */
final class ClientSession {
	/**
	 * The copy's mode, which starts as Off each time the game starts and stays as the player last set it while
	 * the game runs, across copies, servers and dimensions, as Satisfactory keeps a build mode until R changes it.
	 */
	private static CopyMode copyMode = CopyMode.OFF;
	private final int maxEditBlocks;
	/** Where the session's payloads go. */
	private final SessionLink link;
	private final EditGlow glow;
	private final Tiles tiles;
	private final SelfCheck check;
	private final LayerReplica replica;
	private final Clipboard clipboard = new Clipboard();
	/** The build modes' lock on the first click's position (see "Locking and the first click"). */
	private final ShapeLock lock = new ShapeLock();
	/** The player's recent pastes in this dimension, which the copy snaps to. */
	private final PasteBoxes pastes = new PasteBoxes();
	/** Whether the copy was over ghost blocks, for these set boxes, these blocks and this layer at this version. */
	private boolean overGhosts;
	private List<CopyPlacement.Spot> ghostsSet;
	private Map<BlockPos, BlockState> ghostsBlocks;
	private Layer ghostsLayer;
	private long ghostsVersion;
	/**
	 * The copy's colour for real blocks and the items its paste needs ({@link CopyColour#counted}), worked out for these
	 * set boxes and these blocks at this client tick, since the world, the player's eyes and the inventory it reads
	 * change from tick to tick.
	 */
	private CopyColour.Counted realCounted;
	private List<CopyPlacement.Spot> realSet;
	private Map<BlockPos, BlockState> realBlocks;
	private long realTick;
	private final BoxDraw box;
	private final BoxList list = new BoxList();
	/** Whether Delete is on. */
	private boolean deleteOn;
	/** Delete's marks, cleared when Delete ends. */
	private final DeleteMarks marks;
	/** Delete's box, drawn while the left button is held with the sprint key. */
	private final MarkBox markBox = new MarkBox();
	/** The layer, and its version, the marks were last checked against. */
	private Layer marksLayer;
	private long marksVersion;
	/** Delete's hold of the left button that erases the marks. */
	private final EraseHold hold = new EraseHold();
	private final ListSync lists;
	private int clientSeq;
	/**
	 * Whether to remove old plans' placements at the next tick: at the start of a session with a server that runs
	 * Building Planner, and after each change of level, since Litematica restores each dimension's saved placements when
	 * the player enters it. A local session leaves the player's placements on other servers as they are.
	 */
	private boolean removeOldPlans;
	/** The player's settings, which keep the show or hide switch. */
	private final ClientConfig config;
	/** The show or hide key's name, for the message when drawing shows ghost blocks again. */
	private final Supplier<String> showHideKey;
	/** Whether drawing has shown hidden ghost blocks again in this session, which says so only the first time. */
	private boolean toldShownAgain;

	/**
	 * {@code link} is where the session's payloads go, {@code dimension} the game's dimension now (null before it has a
	 * level), and {@code loadedChunks} the chunks of its level already loaded, by {@code Layer.key}. Ghost blocks show or
	 * stay hidden as {@code config} keeps the switch.
	 */
	ClientSession(int maxEditBlocks, SessionLink link, EditGlow glow, Identifier dimension, LongSet loadedChunks,
		ClientConfig config, Supplier<String> showHideKey) {
		this.maxEditBlocks = maxEditBlocks;
		this.link = link;
		this.lists = new ListSync(link);
		this.removeOldPlans = !link.local();
		this.box = new BoxDraw();
		this.marks = new DeleteMarks(maxEditBlocks);
		this.glow = glow;
		this.config = config;
		this.showHideKey = showHideKey;
		this.tiles = new Tiles(GhostsShown.get(config));
		this.check = new SelfCheck(tiles);
		this.replica = new LayerReplica(dimension, new LayerReplica.Listener() {
			@Override
			public void columnChanging(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> changing) {
				check.changing(chunkX, chunkZ, column, changing);
			}

			@Override
			public void columnChanged(int chunkX, int chunkZ, LayerColumn column, List<BlockPos> changed) {
				tiles.columnChanged(chunkX, chunkZ, column, changed);
				check.changed(ClientSession.this.replica.dimension(), chunkX, chunkZ, column, changed);
			}
		});
		for (long key : loadedChunks) {
			replica.chunkLoaded(Layer.keyX(key), Layer.keyZ(key));
		}
	}

	LayerReplica replica() {
		return replica;
	}

	Tiles tiles() {
		return tiles;
	}

	Clipboard clipboard() {
		return clipboard;
	}

	/** The build modes' lock (see "Locking and the first click"). */
	ShapeLock lock() {
		return lock;
	}

	/** The material lists the server keeps for the player. */
	ListSync lists() {
		return lists;
	}

	void chunkLoaded(int chunkX, int chunkZ) {
		replica.chunkLoaded(chunkX, chunkZ);
	}

	void chunkUnloaded(int chunkX, int chunkZ) {
		replica.chunkUnloaded(chunkX, chunkZ);
	}

	/** Where the session's payloads go. */
	SessionLink link() {
		return link;
	}

	/** The game's level changed; Litematica clears every placement then, the tiles among them. */
	void levelChanged(Identifier dimension) {
		removeOldPlans = !link.local();
		replica.levelChanged(dimension);
		check.clear();
		box.end();
		endDelete();
		clipboard.clear();
		lock.unlock();
		pastes.clear();
		glow.clear();
	}

	/**
	 * Once a client tick: removes old plans' placements when due, asks for the columns that loaded and lets go of those
	 * that unloaded, runs the self-check's comparisons that are due, moves Delete's box to where the player aims and
	 * marks it once the left button ({@code attackDown}) is let go, counts Delete's hold of the left button and erases
	 * the marks once it's held a second, moves the box being drawn and the copy at the aim to where the player aims, ends the build modes' lock once a mouse mode is on, and sends the
	 * current material list when it changed.
	 */
	void tick(boolean attackDown) {
		if (removeOldPlans && Minecraft.getInstance().level != null) {
			removeOldPlans = false;
			OldPlans.remove();
		}
		sendWatches();
		check.tick(replica);
		clipboard.tick();
		if (box.on()) {
			box.update(replica.layer(), tiles.shown());
		}
		keepMarks();
		if (markBox.on()) {
			if (attackDown) {
				markBox.update(marks, replica.layer(), tiles.shown());
			} else {
				markBox.release(marks, replica.layer(), tiles.shown());
			}
		}
		if (hold.tick(attackDown) && deleteOn) {
			eraseMarks();
		}
		if (markBox.on()) {
			list.update(markBox.shown(), replica.layer(), tiles.shown());
		} else if (deleteOn) {
			list.update(marks, replica.layer());
		} else {
			list.update(box.shown(), replica.layer(), tiles.shown());
		}
		followAim();
		if (mouseMode() != null) {
			lock.unlock();
		}
		lists.tick(System.currentTimeMillis());
	}

	/**
	 * Moves the copy at the aim against the face the player aims at, flush to a recent paste aimed at, and in line with
	 * the most recent paste while the sprint key is held ({@link PasteBoxes#place}); aiming at nothing, or the copy locked,
	 * leaves it. With a set fixed, the first copy stays and the set follows the aim instead.
	 */
	private void followAim() {
		if (clipboard.setFixed()) {
			followAimWithSet();
			return;
		}
		if (!clipboard.atAim() || clipboard.locked()) {
			return;
		}
		BlockHitResult hit = GhostAim.aim(replica.layer(), tiles.shown(), GhostAim.COPY_REACH);
		if (hit != null) {
			clipboard.moveTo(pastes.place(hit.getBlockPos(), hit.getDirection(), clipboard.spot().size(),
				Minecraft.getInstance().options.keySprint.isDown()).min());
		}
	}

	/**
	 * Lays the fixed set from the first copy to the block Effortless Building's Line, Wall or Floor gives from the first
	 * copy's middle ({@link EffortlessShapes#copyReach}, with its reach and aiming from the camera, hooks E25 and E26),
	 * as {@link CopyGrid#of} counts the copies; where it gives no block, the set stays as it was.
	 */
	private void followAimWithSet() {
		CopyPlacement.Spot first = clipboard.spot();
		BlockPos reached = EffortlessShapes.copyReach(copyMode, first.middle());
		if (reached != null) {
			clipboard.showSet(CopyGrid.of(copyMode, first, reached, CopyGrid.MAX_COPIES));
		}
	}

	/** The copy's mode. */
	static CopyMode copyMode() {
		return copyMode;
	}

	/**
	 * "Change copy mode" with the copy at the aim: Off, Line, Wall and Floor, in that order. During a set, after its
	 * first click, the next mode keeps the same first copy; at Off the set drops and the copy follows the aim.
	 */
	void cycleCopyMode() {
		copyMode = copyMode.next();
		if (copyMode == CopyMode.OFF) {
			dropSet();
		} else {
			followAim();
		}
	}

	/**
	 * The left click during a set, after its first click: drops the set and leaves the copy at the aim in the same mode,
	 * ready for a new set, following the aim again unless it's locked. Returns whether there was a set.
	 */
	boolean dropSet() {
		if (!clipboard.setFixed()) {
			return false;
		}
		clipboard.dropSet();
		followAim();
		return true;
	}

	/** Building Planner's mode that takes over the mouse now, or null. */
	MouseMode mouseMode() {
		return box.on() ? MouseMode.BOX : clipboard.atAim() ? MouseMode.COPY_AT_AIM
			: deleteOn ? MouseMode.DELETE : null;
	}

	/**
	 * The player did something that may end the mouse mode ({@link MouseMode#endedBy}): ending it ends the box, or puts
	 * the copy away. Choosing a build mode also ends the lock.
	 */
	void modeAction(MouseMode.Action action) {
		if (action == MouseMode.Action.CHOOSE_BUILD_MODE) {
			lock.unlock();
		}
		MouseMode mode = mouseMode();
		if (mode != null && mode.endedBy(action)) {
			switch (mode) {
				case BOX -> box.end();
				case COPY_AT_AIM -> clipboard.putAway();
				case DELETE -> endDelete();
			}
		}
	}

	/** Whether Delete is on. */
	boolean deleteOn() {
		return deleteOn;
	}

	/** Delete's marks. */
	DeleteMarks marks() {
		return marks;
	}

	/** Delete's box, for the in-game checks. */
	MarkBox markBox() {
		return markBox;
	}

	/**
	 * The Delete button, with no other mouse mode on: starts Delete with no marks, showing hidden ghost blocks first, as
	 * starting a shape does.
	 */
	void startDelete() {
		showForDrawing();
		marks.clear();
		deleteOn = true;
	}

	/**
	 * The right click in Delete: ends the box being drawn, marking nothing, or clears the marks; returns whether it did
	 * either, false with no box and no marks, or out of Delete.
	 */
	boolean clearMarks() {
		if (!deleteOn) {
			return false;
		}
		if (markBox.on()) {
			markBox.end();
			return true;
		}
		if (marks.size() == 0) {
			return false;
		}
		marks.clear();
		hold.cancel();
		return true;
	}

	/**
	 * The left click in Delete (hook M1): with the sprint key held ({@code sprint}), starts Delete's box at the block
	 * aimed at; otherwise, with marks, starts the hold that erases them once the button has stayed down a second; with
	 * none, does nothing.
	 */
	void deletePress(boolean sprint) {
		if (!deleteOn) {
			return;
		}
		if (sprint) {
			hold.cancel();
			markBox.start(marks, replica.layer(), tiles.shown());
		} else if (marks.size() > 0) {
			hold.press();
		}
	}

	/**
	 * The hold's erase: one erase edit of the marked ghost blocks, with the other half of each two-block ghost block
	 * ({@link GhostErase#of}), so one undo brings them all back, saying how many. The marks go with it, so an undo
	 * brings the ghost blocks back unmarked, and Delete stays on to mark more.
	 */
	private void eraseMarks() {
		List<BlockPos> marked = new ArrayList<>(marks.size());
		marks.positions().forEach((long key) -> marked.add(BlockPos.of(key)));
		Map<BlockPos, BlockState> erase = GhostErase.of(replica.layer(), marked);
		if (sendShape(EditKind.ERASE, erase)) {
			show(PlayerMessage.erased(erase.size()));
			marks.clear();
		}
	}

	/** Delete as its controls lines tell it ({@link ControlsLine#delete}), or null while it's off. */
	ControlsLine.DeleteState deleteState() {
		if (!deleteOn) {
			return null;
		}
		BoxDraw.Shown shown = markBox.shown();
		return new ControlsLine.DeleteState(marks.size() > 0, marks.sweeping() && marks.unmarking(), marks.full(),
			shown == null ? null : new ControlsLine.DeleteBox(markBox.unmarks(),
				BoxCorners.size(shown.min(), shown.max()), shown.limit()));
	}

	/**
	 * The ticks the left button has been held toward the erase, for the progress bar, or -1 while it isn't held to
	 * erase.
	 */
	int eraseTicks() {
		return deleteOn ? hold.ticks() : -1;
	}

	private void endDelete() {
		deleteOn = false;
		markBox.end();
		marks.endSweep();
		marks.clear();
		hold.cancel();
	}

	/**
	 * Once a tick: clears the marks once Delete has ended, and drops each mark whose ghost block has left the layer
	 * since it last changed ({@link DeleteMarks#retain}).
	 */
	private void keepMarks() {
		if (!deleteOn()) {
			marks.clear();
		}
		Layer layer = replica.layer();
		if (marks.size() > 0 && (layer != marksLayer || layer.version() != marksVersion)) {
			marks.retain(layer);
		}
		marksLayer = layer;
		marksVersion = layer.version();
		if (!deleteOn || marks.size() == 0) {
			hold.cancel();
		}
	}

	/** The box being drawn now, for Copy or the material list or Delete's box, as it shows, or null. */
	BoxDraw.Shown boxShown() {
		return box.on() ? box.shown() : markBox.shown();
	}

	/** The kinds of block in the box being drawn, or Delete's marks, for the list under the crosshair. */
	List<BoxList.Entry> boxList() {
		return list.entries();
	}

	/** The box, whose purpose picks the radial menu button that shows selected, and for the in-game checks. */
	BoxDraw box() {
		return box;
	}

	/**
	 * Copy's or the material list's button: starts a box for that purpose, or, while a box is drawn, ends it when it's
	 * for the same purpose and otherwise switches it to that purpose, keeping its corners. Returns whether a box
	 * started.
	 */
	boolean boxButton(BoxPurpose purpose) {
		boolean started = !box.on();
		if (!started && box.purpose() == purpose) {
			box.end();
			return false;
		}
		if (started) {
			box.start(purpose);
		} else {
			box.switchTo(purpose);
		}
		box.update(replica.layer(), tiles.shown());
		return started;
	}

	/**
	 * The left click while a box is drawn: sets its next corner, and after the third hands the box to Copy or the
	 * material list. Returns whether the click made the copy, which is then at the aim.
	 */
	boolean boxClick() {
		BoxDraw.Finished finished = box.click(replica.layer(), tiles.shown());
		if (finished == null) {
			return false;
		}
		switch (finished.purpose()) {
			case COPY -> copy(finished.min(), finished.max());
			case MATERIAL_LIST -> materialList(finished.min(), finished.max());
		}
		return clipboard.atAim();
	}

	void close() {
		box.end();
		endDelete();
		clipboard.clear();
		lock.unlock();
		tiles.clear();
		check.clear();
		glow.clear();
	}

	void receive(Payloads.Columns columns) {
		replica.receive(columns);
		raiseCopy();
	}

	/**
	 * An edit the server applied: into the copy and the tiles, glowing when another player made it, while ghost blocks
	 * show. Ghost blocks real blocks filled go without a glow.
	 */
	void receive(Payloads.Applied applied) {
		List<LayerChange> changes = replica.receive(applied);
		raiseCopy();
		Minecraft minecraft = Minecraft.getInstance();
		if (!changes.isEmpty() && tiles.shown() && minecraft.player != null
			&& !applied.author().equals(minecraft.player.getUUID()) && !applied.author().equals(Payloads.FILLED_AUTHOR)) {
			glow.add(changes.stream().map(LayerChange::pos).toList(), applied.author());
		}
	}

	/** Whether ghost blocks show for this player. */
	boolean ghostsShown() {
		return tiles.shown();
	}

	/**
	 * The show or hide switch: hides ghost blocks while they show, and shows them while they're hidden, saying which in
	 * chat.
	 */
	void toggleGhosts() {
		modeAction(MouseMode.Action.SHOW_OR_HIDE);
		boolean shown = !tiles.shown();
		setGhostsShown(shown);
		show(shown ? PlayerMessage.ghostsShown() : PlayerMessage.ghostsHidden());
	}

	/**
	 * Shows hidden ghost blocks before the player draws (a shape starting or finishing, or a paste), saying so the first
	 * time in a session.
	 */
	void showForDrawing() {
		if (tiles.shown()) {
			return;
		}
		setGhostsShown(true);
		if (!toldShownAgain) {
			toldShownAgain = true;
			show(PlayerMessage.ghostsShownAgain(showHideKey.get()));
		}
	}

	private void setGhostsShown(boolean shown) {
		tiles.setShown(shown);
		if (!shown) {
			glow.clear();
		}
		raiseCopy();
		try {
			GhostsShown.set(config, shown);
		} catch (IOException e) {
			BuildingPlanner.LOGGER.warn("Couldn't save whether ghost blocks show", e);
		}
	}

	/**
	 * Sends a shape the player drew, or a paste, as one edit, {@code blocks} being world positions and states in the
	 * order drawn (air to erase), after showing hidden ghost blocks. A shape over the server's block cap isn't sent,
	 * with the server's message. Returns whether it was sent.
	 */
	boolean sendShape(EditKind kind, Map<BlockPos, BlockState> blocks) {
		if (blocks.isEmpty() || replica.dimension() == null) {
			return false;
		}
		showForDrawing();
		if (blocks.size() > maxEditBlocks) {
			show(PlayerMessage.tooManyBlocks(blocks.size(), maxEditBlocks));
			return false;
		}
		List<LayerChange> changes = new ArrayList<>(blocks.size());
		blocks.forEach((pos, state) -> changes.add(new LayerChange(pos.immutable(), state)));
		// Columns loaded this tick are asked for first, as the server checks an edit's columns are watched.
		sendWatches();
		link.send(new Payloads.Edit(replica.dimension(), ++clientSeq, kind, changes));
		return true;
	}

	void undo() {
		link.send(new Payloads.Undo());
	}

	void redo() {
		link.send(new Payloads.Redo());
	}

	/**
	 * Makes the copy from a box drawn for Copy: at each position the ghost block from the player's copy of the layer,
	 * and where there is none, the real block from the player's world other than air, without block entity contents
	 * ({@link CopyRules#blocks}), while ghost blocks are hidden only the real blocks. The copy then shows at the aim. A
	 * box with nothing to copy, or more blocks than the server's cap for an edit, which its paste would exceed, leaves
	 * any earlier copy, with a message; the blocks are counted ({@link BoxCount}) before any is taken, so a large box
	 * over the cap takes none.
	 */
	void copy(BlockPos min, BlockPos max) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		BoxCount.Sections world = BoxList.realSections(level);
		int count = BoxCount.of(min, max, replica.layer(), world, tiles.shown()).values().intStream().sum();
		Optional<PlayerMessage> refusal = CopyRules.refusal(count, maxEditBlocks);
		if (refusal.isPresent()) {
			show(refusal.get());
			return;
		}
		Map<BlockPos, BlockState> blocks = CopyRules.blocks(min, max, replica.layer(), world, tiles.shown());
		clipboard.place(blocks);
		followAim();
		if (clipboard.atAim()) {
			show(PlayerMessage.copied(blocks.size()));
		}
	}

	/**
	 * Opens Litematica's material list for the blocks in the box from {@code min} to {@code max}, ghost blocks and real
	 * blocks alike, leaving hidden ghost blocks hidden, and makes it the current list, the one Litematica's own material
	 * list key opens, which the server keeps as "Last counted". A box with no blocks, or while ghost blocks are hidden
	 * with no real block, opens an empty list.
	 */
	void materialList(BlockPos min, BlockPos max) {
		if (replica.dimension() == null) {
			return;
		}
		GhostMaterialList list = new GhostMaterialList(replica.dimension(), min, max);
		lists.makeCurrent(list);
		GuiBase.openGui(new GuiMaterialList(list));
	}

	/**
	 * The right click with the copy at the aim. In Off it pastes the copy as one shape edit. In Line, Wall and Floor the
	 * first click fixes the copy as the set's first, and the second pastes the whole set as one shape edit, which one
	 * undo takes back. A paste names only the copies' blocks, so their empty space leaves the layer's, and each copy's
	 * box joins the recent pastes the copy snaps to, as a paste of its own, in the set's order; the copy stays at the
	 * aim, unlocked, and follows the aim for the next paste. While "Place real blocks" acts, the paste goes to the server
	 * as one {@code place} of real blocks instead ({@link #sendPlace}). While the copy, or its set, is red
	 * ({@link #copyShown}), the click that would paste pastes nothing and sends nothing; a set's first click, which
	 * pastes nothing anyway, still fixes the first copy.
	 */
	void paste() {
		if (!clipboard.atAim()) {
			return;
		}
		boolean red = copyShown().red();
		if (copyMode == CopyMode.OFF) {
			if (red) {
				return;
			}
			if (sendPaste(clipboard.worldBlocks())) {
				pastes.add(clipboard.spot());
				clipboard.unlock();
				followAim();
			}
		} else if (!clipboard.setFixed()) {
			clipboard.fixSet();
			followAim();
		} else if (!red) {
			List<CopyPlacement.Spot> copies = clipboard.setSpots();
			if (sendPaste(clipboard.setBlocks())) {
				pastes.addAll(copies);
				clipboard.unlock();
				dropSet();
			}
		}
	}

	/**
	 * The colour of the copy at the aim and its set, if one is fixed ({@link CopyColour}), against the world's build
	 * height and border and the server's block cap, and the player's copy of the layer whether or not ghost blocks show,
	 * since a paste shows them again and replaces them. Asked each frame to draw it and at each paste, so whether the
	 * copy is over ghost blocks, which reads each of its blocks, is kept until the set's boxes, the copy, its turn or
	 * the layer change. While "Place real blocks" acts, the colour for real blocks instead ({@link #realShown}). Null
	 * while the copy isn't at the aim.
	 */
	CopyColour.Shown copyShown() {
		ClientLevel level = Minecraft.getInstance().level;
		Player player = Minecraft.getInstance().player;
		if (!clipboard.atAim() || level == null || player == null) {
			return null;
		}
		List<CopyPlacement.Spot> set = clipboard.setSpots();
		Map<BlockPos, BlockState> blocks = clipboard.blocksFromCorner();
		if (BuildingPlannerClient.realActs()) {
			return realCounted(level, player, set, blocks).shown();
		}
		Layer layer = replica.layer();
		return CopyColour.of(set, blocks.size(), world(level), maxEditBlocks, () -> {
			if (!set.equals(ghostsSet) || blocks != ghostsBlocks || layer != ghostsLayer
				|| layer.version() != ghostsVersion) {
				overGhosts = CopyColour.overGhosts(set, blocks, layer);
				ghostsSet = set;
				ghostsBlocks = blocks;
				ghostsLayer = layer;
				ghostsVersion = layer.version();
			}
			return overGhosts;
		});
	}

	/**
	 * A paste of {@code blocks} (world positions and states): as one {@code place} of real blocks while "Place real
	 * blocks" acts, otherwise as one shape edit. Returns whether it was sent.
	 */
	private boolean sendPaste(Map<BlockPos, BlockState> blocks) {
		return BuildingPlannerClient.realActs() ? sendPlace(blocks) : sendShape(EditKind.SHAPE, blocks);
	}

	/**
	 * Sends a paste's real blocks as one {@code place} (hook M2; see "Place real blocks": "A paste"): each block of the
	 * copy as {@code blocks} gives them, with the top face and a hit at the bottom middle of its first block, as if each
	 * block were placed onto the block under it. Hidden ghost blocks stay hidden, and the paste makes no undo step.
	 * Returns whether it was sent.
	 */
	private boolean sendPlace(Map<BlockPos, BlockState> blocks) {
		if (blocks.isEmpty() || replica.dimension() == null) {
			return false;
		}
		List<LayerChange> changes = new ArrayList<>(blocks.size());
		blocks.forEach((pos, state) -> changes.add(new LayerChange(pos.immutable(), state)));
		link.send(new Payloads.Place(replica.dimension(), Direction.UP,
			Vec3.atBottomCenterOf(changes.getFirst().pos()), true, changes));
		return true;
	}

	/**
	 * The copy's colour for real blocks and the items its paste needs ({@link CopyColour#counted}), against the real
	 * world as the player's game has it, the reach from the player's eyes (not Freecam's camera) and the player's
	 * inventory, worked out once a client tick for the set's boxes and the copy's blocks, as the shape's preview is.
	 */
	private CopyColour.Counted realCounted(ClientLevel level, Player player, List<CopyPlacement.Spot> set,
		Map<BlockPos, BlockState> blocks) {
		long tick = level.getGameTime();
		if (realCounted == null || tick != realTick || blocks != realBlocks || !set.equals(realSet)) {
			realCounted = CopyColour.counted(set, blocks, realWorld(level, player), player.getEyePosition(),
				RealPlace.Inventory.of(player));
			realTick = tick;
			realBlocks = blocks;
			realSet = set;
		}
		return realCounted;
	}

	/**
	 * The items a paste of the copy at the aim and its set uses while "Place real blocks" acts, for the click's panel:
	 * the server's all-or-nothing count ({@link CopyColour#counted}), the set of a Line, Wall or Floor of copies once its
	 * first click has fixed it and the one copy before; null while the copy isn't at the aim, the setting doesn't act,
	 * or the paste is past the cap, which the server refuses before it counts the inventory.
	 */
	Map<Item, Integer> copyNeeds() {
		ClientLevel level = Minecraft.getInstance().level;
		Player player = Minecraft.getInstance().player;
		if (!clipboard.atAim() || level == null || player == null || !BuildingPlannerClient.realActs()) {
			return null;
		}
		return realCounted(level, player, clipboard.setSpots(), clipboard.blocksFromCorner()).needs();
	}

	/**
	 * The world as the player's game checks a real block before placing it (see "Place real blocks"): within the build
	 * height and inside the world border, the real block there, and whether an entity is in its way, as Minecraft checks
	 * it for the player's placing.
	 */
	static RealPlace.World realWorld(ClientLevel level, Player player) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return level.getBlockState(pos);
			}

			@Override
			public boolean unobstructed(RealPlace.Entry block) {
				return level.isUnobstructed(block.state(), block.pos(), CollisionContext.placementContext(player));
			}
		};
	}

	/**
	 * The colour of the locked positions ({@link CopyColour#ofBlocks}): the positions {@code own} and the blocks a right
	 * click would place there, against the world's build height and border, the server's block cap, and the player's
	 * copy of the layer whether or not ghost blocks show.
	 */
	CopyColour.Shown lockColour(List<BlockPos> own, Map<BlockPos, BlockState> placed) {
		return CopyColour.ofBlocks(own, placed, world(Minecraft.getInstance().level), maxEditBlocks, replica.layer());
	}

	/** The world's limits on where a block may go, as the server checks an edit's positions. */
	private static CopyColour.World world(ClientLevel level) {
		return new CopyColour.World() {
			@Override
			public boolean inBuildHeight(int y) {
				return !level.isOutsideBuildHeight(y);
			}

			@Override
			public boolean insideBorder(BlockPos pos) {
				return level.getWorldBorder().isWithinBounds(pos);
			}
		};
	}

	/**
	 * The copy at the aim as its controls line tells it ({@link ControlsLine#copy}): its mode, whether it's locked, the
	 * copies in a fixed set, whether the player has pasted in this dimension, and why its paste would be refused. Null
	 * while the copy isn't at the aim.
	 */
	ControlsLine.CopyState copyState() {
		CopyColour.Shown shown = copyShown();
		if (shown == null) {
			return null;
		}
		return new ControlsLine.CopyState(copyMode, clipboard.locked(),
			clipboard.setFixed() ? clipboard.setSpots().size() : 0, pastes.latest() != null, shown.refusal());
	}

	/**
	 * Brings the most recent copy back to the aim, as it was turned and mirrored; returns whether there was one. With
	 * none since the player joined, says so.
	 */
	boolean bringBackCopy() {
		if (!clipboard.exists()) {
			show(PlayerMessage.nothingToPaste());
			return false;
		}
		clipboard.bringBack();
		followAim();
		return true;
	}

	/**
	 * Gives the copy at the aim a quarter turn around its middle, clockwise or counterclockwise seen from above, and sets
	 * it against the aim again unless it's locked; with a set fixed, the first copy turns in place and the set's other
	 * copies turn with it, laid side by side again at their turned size.
	 */
	void turnCopy(boolean clockwise) {
		if (clipboard.atAim()) {
			clipboard.turn(clockwise);
			followAim();
		}
	}

	/** Flips the copy at the aim left to right for a view facing {@code facing}, around its middle. */
	void mirrorCopy(Direction facing) {
		if (clipboard.atAim()) {
			clipboard.mirror(facing);
			followAim();
		}
	}

	/** "Lock or unlock" with the copy at the aim: the copy stops following the aim, or follows it again. */
	void toggleCopyLock() {
		clipboard.toggleLock();
		followAim();
	}

	/** A "Nudge" key with the copy at the aim: moves the locked copy one block that way. */
	void nudgeCopy(Direction direction) {
		if (clipboard.locked()) {
			clipboard.nudge(direction);
		}
	}

	/** Shows one of Building Planner's messages in the player's chat. */
	static void show(PlayerMessage message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.sendSystemMessage(Component.translatable(message.key(), message.args().toArray()));
		}
	}

	private void sendWatches() {
		for (CustomPacketPayload message : replica.takeMessages()) {
			link.send(message);
		}
	}

	/** Litematica draws placements in the order they were added, so the copy goes after any new tile. */
	private void raiseCopy() {
		if (tiles.takeAdded()) {
			clipboard.raise();
		}
	}
}
