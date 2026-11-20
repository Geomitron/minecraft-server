package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.input.MouseMode;
import dev.buildingplanner.layer.BoxCount;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.layer.LayerColumn;
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
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building Planner on one connection to a server that has it: the player's copy of the layer, kept equal to the
 * server's in the columns the game has loaded, drawn through Litematica's tiles. Everything runs on the client's main
 * thread, where Fabric runs the payload handlers.
 */
final class ClientSession {
	private final int maxEditBlocks;
	private final EditGlow glow;
	private final Tiles tiles;
	private final SelfCheck check;
	private final LayerReplica replica;
	private final Clipboard clipboard = new Clipboard();
	private final BoxDraw box = new BoxDraw();
	private final BoxList list = new BoxList();
	private final ListSync lists = new ListSync();
	private int clientSeq;
	/**
	 * Whether to remove old plans' placements at the next tick: at the session's start, and after each change of level,
	 * since Litematica restores each dimension's saved placements when the player enters it.
	 */
	private boolean removeOldPlans = true;
	/** The player's settings, which keep the show or hide switch. */
	private final ClientConfig config;
	/** The show or hide key's name, for the message when drawing shows ghost blocks again. */
	private final Supplier<String> showHideKey;
	/** Whether drawing has shown hidden ghost blocks again in this session, which says so only the first time. */
	private boolean toldShownAgain;

	/**
	 * {@code dimension} is the game's dimension now (null before it has a level), and {@code loadedChunks} the chunks of
	 * its level already loaded, by {@code Layer.key}. Ghost blocks show or stay hidden as {@code config} keeps the
	 * switch.
	 */
	ClientSession(int maxEditBlocks, EditGlow glow, Identifier dimension, LongSet loadedChunks, ClientConfig config,
		Supplier<String> showHideKey) {
		this.maxEditBlocks = maxEditBlocks;
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

	/** The game's level changed; Litematica clears every placement then, the tiles among them. */
	void levelChanged(Identifier dimension) {
		removeOldPlans = true;
		replica.levelChanged(dimension);
		check.clear();
		box.end();
		clipboard.clear();
		glow.clear();
	}

	/**
	 * Once a client tick: removes old plans' placements when due, asks for the columns that loaded and lets go of those
	 * that unloaded, runs the self-check's comparisons that are due, moves the box being drawn and the copy at the aim
	 * to where the player aims, and sends the current material list when it changed.
	 */
	void tick() {
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
		list.update(box.shown(), replica.layer(), tiles.shown());
		followAim();
		lists.tick(System.currentTimeMillis());
	}

	/** Moves the copy at the aim to the block beside the face the player aims at; aiming at nothing leaves it. */
	private void followAim() {
		if (clipboard.atAim()) {
			BlockPos origin = GhostAim.copyOrigin(replica.layer(), tiles.shown());
			if (origin != null) {
				clipboard.follow(origin);
			}
		}
	}

	/** Building Planner's mode that takes over the mouse now, or null. */
	MouseMode mouseMode() {
		return box.on() ? MouseMode.BOX : clipboard.atAim() ? MouseMode.COPY_AT_AIM : null;
	}

	/**
	 * The player did something that may end the mouse mode ({@link MouseMode#endedBy}): ending it ends the box, or puts
	 * the copy away.
	 */
	void modeAction(MouseMode.Action action) {
		MouseMode mode = mouseMode();
		if (mode != null && mode.endedBy(action)) {
			switch (mode) {
				case BOX -> box.end();
				case COPY_AT_AIM -> clipboard.putAway();
			}
		}
	}

	/** The box being drawn now, as it shows, or null. */
	BoxDraw.Shown boxShown() {
		return box.shown();
	}

	/** The kinds of block in the box being drawn, for the list under the crosshair. */
	List<BoxList.Entry> boxList() {
		return list.entries();
	}

	/** The box, whose purpose picks the radial menu button that shows selected, and for the in-game checks. */
	BoxDraw box() {
		return box;
	}

	/**
	 * Copy's or the material list's button: starts a box for that purpose, or, while a box is drawn, ends it when it's
	 * for the same purpose and otherwise switches it to that purpose, keeping its corners. Returns whether a box started.
	 */
	boolean boxButton(BoxDraw.Purpose purpose) {
		if (box.on()) {
			if (box.purpose() == purpose) {
				box.end();
			} else {
				box.switchTo(purpose);
			}
			return false;
		}
		box.start(purpose);
		box.update(replica.layer(), tiles.shown());
		return true;
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
		clipboard.clear();
		tiles.clear();
		check.clear();
		glow.clear();
	}

	void receive(Payloads.Columns columns) {
		replica.receive(columns);
		raiseCopy();
	}

	/**
	 * An edit the server applied: into the copy and the tiles, glowing when someone else made it, while ghost blocks
	 * show.
	 */
	void receive(Payloads.Applied applied) {
		List<LayerChange> changes = replica.receive(applied);
		raiseCopy();
		Minecraft minecraft = Minecraft.getInstance();
		if (!changes.isEmpty() && tiles.shown() && minecraft.player != null
			&& !applied.author().equals(minecraft.player.getUUID())) {
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
	 * with the server's message.
	 */
	void sendShape(EditKind kind, Map<BlockPos, BlockState> blocks) {
		if (blocks.isEmpty() || replica.dimension() == null) {
			return;
		}
		showForDrawing();
		if (blocks.size() > maxEditBlocks) {
			show(PlayerMessage.tooManyBlocks(blocks.size(), maxEditBlocks));
			return;
		}
		List<LayerChange> changes = new ArrayList<>(blocks.size());
		blocks.forEach((pos, state) -> changes.add(new LayerChange(pos.immutable(), state)));
		// Columns loaded this tick are asked for first, as the server checks an edit's columns are watched.
		sendWatches();
		ClientPlayNetworking.send(new Payloads.Edit(replica.dimension(), ++clientSeq, kind, changes));
	}

	void undo() {
		ClientPlayNetworking.send(new Payloads.Undo());
	}

	void redo() {
		ClientPlayNetworking.send(new Payloads.Redo());
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
		clipboard.place(blocks, GhostAim.copyOrigin(replica.layer(), tiles.shown()));
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
	 * Pastes the copy at the aim as one shape edit. It names only the copy's blocks, so its empty space leaves the
	 * layer's.
	 */
	void paste() {
		if (clipboard.atAim()) {
			sendShape(EditKind.SHAPE, clipboard.worldBlocks());
		}
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

	/** Gives the copy at the aim a quarter turn, clockwise or counterclockwise seen from above. */
	void turnCopy(boolean clockwise) {
		if (clipboard.atAim()) {
			clipboard.turn(clockwise);
		}
	}

	void mirrorCopy(Direction facing) {
		if (clipboard.atAim()) {
			clipboard.mirror(facing);
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
			ClientPlayNetworking.send(message);
		}
	}

	/** Litematica draws placements in the order they were added, so the copy goes after any new tile. */
	private void raiseCopy() {
		if (tiles.takeAdded()) {
			clipboard.raise();
		}
	}
}
