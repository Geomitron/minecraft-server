package dev.buildingplanner.sync;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.PackedSection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building Planner's network payloads for the shared layer, namespace {@code buildingplanner}, protocol version 3.
 * Dimensions travel as their ids; block states as ids in the block-state registry, since both sides run the same
 * vanilla blocks.
 */
public final class Payloads {
	/** Raised whenever a payload's layout changes; a client that sees another version turns Building Planner off. */
	public static final int PROTOCOL_VERSION = 3;
	/** Marks an {@code applied} that answers no edit of the player's: an undo or a redo. */
	public static final int NO_CLIENT_SEQ = -1;
	/** Room for {@link SyncLimits#WIRE_MAX_EDIT_BLOCKS} changes at their largest (a long and a three-byte id each). */
	static final int MAX_EDIT_BYTES = SyncLimits.WIRE_MAX_EDIT_BLOCKS * 12 + 64 * 1024;
	/** Room for 131,072 columns in one {@code watch} or {@code unwatch}, far more than the largest view distance loads. */
	static final int MAX_WATCH_BYTES = 1024 * 1024;
	/** Column snapshots and applied edits go to players; their size follows the layer's. */
	private static final int MAX_CLIENTBOUND_BYTES = Integer.MAX_VALUE;
	private static final int MAX_NAME_LENGTH = 256;

	private Payloads() {
	}

	// Client to server.

	/** The columns of the player's dimension that their game loaded since its last {@code watch}. */
	public record Watch(Identifier dimension, List<ChunkPos> columns) implements CustomPacketPayload {
		public static final Type<Watch> TYPE = payloadType("watch");
		static final StreamCodec<FriendlyByteBuf, Watch> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				writeList(buf, p.columns, (b, column) -> b.writeChunkPos(column));
			},
			buf -> new Watch(buf.readIdentifier(), readList(buf, b -> b.readChunkPos())));

		public Watch {
			columns = List.copyOf(columns);
		}

		@Override
		public Type<Watch> type() {
			return TYPE;
		}
	}

	/** The columns the player's game unloaded since its last {@code unwatch}. */
	public record Unwatch(Identifier dimension, List<ChunkPos> columns) implements CustomPacketPayload {
		public static final Type<Unwatch> TYPE = payloadType("unwatch");
		static final StreamCodec<FriendlyByteBuf, Unwatch> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				writeList(buf, p.columns, (b, column) -> b.writeChunkPos(column));
			},
			buf -> new Unwatch(buf.readIdentifier(), readList(buf, b -> b.readChunkPos())));

		public Unwatch {
			columns = List.copyOf(columns);
		}

		@Override
		public Type<Unwatch> type() {
			return TYPE;
		}
	}

	/** A shape or an erase, in world positions; an erase's changes are air. */
	public record Edit(Identifier dimension, int clientSeq, EditKind kind, List<LayerChange> changes)
		implements CustomPacketPayload {
		public static final Type<Edit> TYPE = payloadType("edit");
		static final StreamCodec<FriendlyByteBuf, Edit> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				buf.writeVarInt(p.clientSeq);
				buf.writeEnum(p.kind);
				writeChanges(buf, p.changes);
			},
			buf -> new Edit(buf.readIdentifier(), buf.readVarInt(), buf.readEnum(EditKind.class), readChanges(buf)));

		public Edit {
			changes = List.copyOf(changes);
		}

		@Override
		public Type<Edit> type() {
			return TYPE;
		}
	}

	public record Undo() implements CustomPacketPayload {
		public static final Type<Undo> TYPE = payloadType("undo");
		static final StreamCodec<FriendlyByteBuf, Undo> CODEC = StreamCodec.unit(new Undo());

		@Override
		public Type<Undo> type() {
			return TYPE;
		}
	}

	public record Redo() implements CustomPacketPayload {
		public static final Type<Redo> TYPE = payloadType("redo");
		static final StreamCodec<FriendlyByteBuf, Redo> CODEC = StreamCodec.unit(new Redo());

		@Override
		public Type<Redo> type() {
			return TYPE;
		}
	}

	// Server to client.

	/** Sent at join: the server's protocol version and the block cap for an edit. */
	public record Hello(int protocolVersion, int maxEditBlocks) implements CustomPacketPayload {
		public static final Type<Hello> TYPE = payloadType("hello");
		static final StreamCodec<FriendlyByteBuf, Hello> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.protocolVersion);
				buf.writeVarInt(p.maxEditBlocks);
			},
			buf -> new Hello(buf.readVarInt(), buf.readVarInt()));

		@Override
		public Type<Hello> type() {
			return TYPE;
		}
	}

	/**
	 * The answer to a {@code watch}: each column it asked for that the server took, with its sections holding ghost
	 * blocks, lowest first, or none for an empty column.
	 */
	public record Columns(Identifier dimension, List<Column> columns) implements CustomPacketPayload {
		public static final Type<Columns> TYPE = payloadType("columns");
		static final StreamCodec<FriendlyByteBuf, Columns> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				writeList(buf, p.columns, (b, column) -> {
					b.writeChunkPos(column.pos());
					writeList(b, column.sections(), (bb, section) -> {
						bb.writeVarInt(section.y());
						writeList(bb, section.palette(), Payloads::writeState);
						bb.writeLongArray(section.blocks());
					});
				});
			},
			buf -> new Columns(buf.readIdentifier(), readList(buf, b -> new Column(b.readChunkPos(),
				readList(b, bb -> new PackedSection(bb.readVarInt(), readList(bb, Payloads::readState), bb.readLongArray()))))));

		public Columns {
			columns = List.copyOf(columns);
		}

		@Override
		public Type<Columns> type() {
			return TYPE;
		}
	}

	/** One column of a {@link Columns}. */
	public record Column(ChunkPos pos, List<PackedSection> sections) {
		public Column {
			sections = List.copyOf(sections);
		}
	}

	/**
	 * One edit as the server applied it, with the changes that changed a block, to every player who watches a column it
	 * touches, its author included.
	 */
	public record Applied(Identifier dimension, UUID author, int clientSeq, List<LayerChange> changes)
		implements CustomPacketPayload {
		public static final Type<Applied> TYPE = payloadType("applied");
		static final StreamCodec<FriendlyByteBuf, Applied> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				buf.writeUUID(p.author);
				buf.writeVarInt(p.clientSeq);
				writeChanges(buf, p.changes);
			},
			buf -> new Applied(buf.readIdentifier(), buf.readUUID(), buf.readVarInt(), readChanges(buf)));

		public Applied {
			changes = List.copyOf(changes);
		}

		@Override
		public Type<Applied> type() {
			return TYPE;
		}
	}

	/** The server didn't apply the player's edit, and changed nothing. */
	public record Rejected(int clientSeq, PlayerMessage reason) implements CustomPacketPayload {
		public static final Type<Rejected> TYPE = payloadType("rejected");
		static final StreamCodec<FriendlyByteBuf, Rejected> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.clientSeq);
				writeMessage(buf, p.reason);
			},
			buf -> new Rejected(buf.readVarInt(), readMessage(buf)));

		@Override
		public Type<Rejected> type() {
			return TYPE;
		}
	}

	public record Notice(PlayerMessage message) implements CustomPacketPayload {
		public static final Type<Notice> TYPE = payloadType("notice");
		static final StreamCodec<FriendlyByteBuf, Notice> CODEC = StreamCodec.of(
			(buf, p) -> writeMessage(buf, p.message), buf -> new Notice(readMessage(buf)));

		@Override
		public Type<Notice> type() {
			return TYPE;
		}
	}

	/** Registers every payload type; both sides call this at start. */
	public static void register() {
		PayloadTypeRegistry<?> serverbound = PayloadTypeRegistry.serverboundPlay();
		serverbound.registerLarge(Watch.TYPE, Watch.CODEC, MAX_WATCH_BYTES);
		serverbound.registerLarge(Unwatch.TYPE, Unwatch.CODEC, MAX_WATCH_BYTES);
		serverbound.registerLarge(Edit.TYPE, Edit.CODEC, MAX_EDIT_BYTES);
		serverbound.register(Undo.TYPE, Undo.CODEC);
		serverbound.register(Redo.TYPE, Redo.CODEC);
		PayloadTypeRegistry<?> clientbound = PayloadTypeRegistry.clientboundPlay();
		clientbound.register(Hello.TYPE, Hello.CODEC);
		clientbound.registerLarge(Columns.TYPE, Columns.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.registerLarge(Applied.TYPE, Applied.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.register(Rejected.TYPE, Rejected.CODEC);
		clientbound.register(Notice.TYPE, Notice.CODEC);
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, path));
	}

	private static void writeChanges(FriendlyByteBuf buf, List<LayerChange> changes) {
		writeList(buf, changes, (b, change) -> {
			b.writeBlockPos(change.pos());
			writeState(b, change.state());
		});
	}

	private static List<LayerChange> readChanges(FriendlyByteBuf buf) {
		return readList(buf, b -> new LayerChange(b.readBlockPos(), readState(b)));
	}

	static void writeState(FriendlyByteBuf buf, BlockState state) {
		buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(state));
	}

	static BlockState readState(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
		if (state == null) {
			throw new IllegalArgumentException("Unknown block state id " + id);
		}
		return state;
	}

	static void writeMessage(FriendlyByteBuf buf, PlayerMessage message) {
		buf.writeUtf(message.key(), MAX_NAME_LENGTH);
		writeList(buf, message.args(), (b, arg) -> b.writeUtf(arg, MAX_NAME_LENGTH));
	}

	static PlayerMessage readMessage(FriendlyByteBuf buf) {
		return new PlayerMessage(buf.readUtf(MAX_NAME_LENGTH), readList(buf, b -> b.readUtf(MAX_NAME_LENGTH)));
	}

	interface Writer<T> {
		void write(FriendlyByteBuf buf, T value);
	}

	static <T> void writeList(FriendlyByteBuf buf, List<T> values, Writer<T> writer) {
		buf.writeVarInt(values.size());
		for (T value : values) {
			writer.write(buf, value);
		}
	}

	/** Doesn't trust the count for the list's capacity: a short buffer runs out before a false count does harm. */
	static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		int count = buf.readVarInt();
		List<T> values = new ArrayList<>(Math.min(Math.max(count, 0), 1024));
		for (int i = 0; i < count; i++) {
			values.add(reader.apply(buf));
		}
		return values;
	}
}
