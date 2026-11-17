package dev.buildingplanner.client;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.ClientConfig;
import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.look.GhostsShown;
import dev.buildingplanner.sync.CopyRules;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.LayerReplica;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.data.DataManager;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
	private final LayerReplica replica;
	private final Clipboard clipboard = new Clipboard();
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
		this.replica = new LayerReplica(dimension, tiles);
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
		clipboard.clear();
		glow.clear();
	}

	/**
	 * Once a client tick: removes old plans' placements when due, asks for the columns that loaded and lets go of those
	 * that unloaded, and moves the copy to where the player aims.
	 */
	void tick() {
		if (removeOldPlans && Minecraft.getInstance().level != null) {
			removeOldPlans = false;
			OldPlans.remove();
		}
		sendWatches();
		if (clipboard.exists()) {
			BlockPos origin = GhostAim.copyOrigin(replica.layer(), tiles.shown());
			if (origin != null) {
				clipboard.follow(origin);
			}
		}
	}

	void close() {
		clipboard.clear();
		tiles.clear();
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
	 * ({@link CopyRules#blocks}); {@code pasteKey} is the Paste key's name, for the message. A box with nothing to copy,
	 * or more blocks than the server's cap for an edit, which its paste would exceed, leaves any earlier copy, with a
	 * message.
	 */
	void copy(BlockPos min, BlockPos max, String pasteKey) {
		Level level = Minecraft.getInstance().level;
		Map<BlockPos, BlockState> blocks = CopyRules.blocks(min, max, replica.layer(),
			pos -> level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos));
		Optional<PlayerMessage> refusal = CopyRules.refusal(blocks.size(), maxEditBlocks);
		if (refusal.isPresent()) {
			show(refusal.get());
			return;
		}
		clipboard.place(blocks, GhostAim.copyOrigin(replica.layer(), tiles.shown()));
		if (clipboard.exists()) {
			show(PlayerMessage.copied(blocks.size(), pasteKey));
		}
	}

	/**
	 * Opens Litematica's material list for the ghost blocks in the box from {@code min} to {@code max}, after showing
	 * hidden ghost blocks, and makes it the list Litematica's own material list key opens. A box with no ghost blocks
	 * opens nothing, with a message.
	 */
	void materialList(BlockPos min, BlockPos max) {
		if (replica.dimension() == null) {
			return;
		}
		showForDrawing();
		GhostMaterialList list = new GhostMaterialList(replica.dimension(), min, max);
		if (list.getCountTotal() == 0) {
			show(PlayerMessage.nothingToList());
			return;
		}
		DataManager.setMaterialList(list);
		GuiBase.openGui(new GuiMaterialList(list));
	}

	/** Pastes the copy as one shape edit. It names only the copy's blocks, so its empty space leaves the layer's. */
	void paste(String copyKey) {
		if (!clipboard.exists()) {
			show(PlayerMessage.nothingToPaste(copyKey));
			return;
		}
		sendShape(EditKind.SHAPE, clipboard.worldBlocks());
	}

	void turnCopy() {
		if (clipboard.exists()) {
			clipboard.turn();
		}
	}

	void mirrorCopy(Direction facing) {
		if (clipboard.exists()) {
			clipboard.mirror(facing);
		}
	}

	void clearCopy() {
		clipboard.clear();
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
