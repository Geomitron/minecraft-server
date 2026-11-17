package dev.buildingplanner.client;

import dev.buildingplanner.layer.Layer;
import dev.buildingplanner.sync.CopyRules;
import dev.buildingplanner.sync.EditKind;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.LayerReplica;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
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
	private final Tiles tiles = new Tiles();
	private final LayerReplica replica;
	private final Clipboard clipboard = new Clipboard();
	private int clientSeq;

	/**
	 * {@code dimension} is the game's dimension now (null before it has a level), and {@code loadedChunks} the chunks of
	 * its level already loaded, by {@code Layer.key}.
	 */
	ClientSession(int maxEditBlocks, EditGlow glow, Identifier dimension, LongSet loadedChunks) {
		this.maxEditBlocks = maxEditBlocks;
		this.glow = glow;
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
		replica.levelChanged(dimension);
		clipboard.clear();
		glow.clear();
	}

	/** Once a client tick: asks for the columns that loaded and lets go of those that unloaded. */
	void tick() {
		sendWatches();
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

	/** An edit the server applied: into the copy and the tiles, glowing when someone else made it. */
	void receive(Payloads.Applied applied) {
		List<LayerChange> changes = replica.receive(applied);
		raiseCopy();
		Minecraft minecraft = Minecraft.getInstance();
		if (!changes.isEmpty() && minecraft.player != null && !applied.author().equals(minecraft.player.getUUID())) {
			glow.add(changes.stream().map(LayerChange::pos).toList(), applied.author());
		}
	}

	/**
	 * Sends a shape the player drew as one edit, {@code blocks} being world positions and states in the order drawn
	 * (air to erase). A shape over the server's block cap isn't sent, with the server's message.
	 */
	void sendShape(EditKind kind, Map<BlockPos, BlockState> blocks) {
		if (blocks.isEmpty() || replica.dimension() == null) {
			return;
		}
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
	 * Copies the ghost blocks in the area marked with Litematica's area tool; {@code pasteKey} is the Paste key's name,
	 * for the message.
	 */
	void copy(String pasteKey) {
		AreaSelection area = StartPlanScreen.markedArea();
		if (area == null) {
			show(PlayerMessage.noAreaMarked());
			return;
		}
		Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		for (Box box : PositionUtils.getValidBoxes(area)) {
			if (box.getPos1() != null && box.getPos2() != null) {
				replica.layer().forEachIn(PositionUtils.getMinCorner(box.getPos1(), box.getPos2()),
					PositionUtils.getMaxCorner(box.getPos1(), box.getPos2()), blocks::put);
			}
		}
		Optional<PlayerMessage> refusal = CopyRules.refusal(blocks.size(), maxEditBlocks);
		if (refusal.isPresent()) {
			show(refusal.get());
			return;
		}
		clipboard.place(blocks);
		if (clipboard.exists()) {
			show(PlayerMessage.copied(blocks.size(), pasteKey));
		}
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
