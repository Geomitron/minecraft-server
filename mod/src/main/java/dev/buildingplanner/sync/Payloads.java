package dev.buildingplanner.sync;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.OpenChoices;
import dev.buildingplanner.plan.RegionSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Building Planner's network payloads, namespace {@code buildingplanner}. Block states travel as ids in the
 * block-state registry; both sides run the same vanilla blocks.
 */
public final class Payloads {
	/** Raised whenever a payload's layout changes; a client that sees another version turns Building Planner off. */
	public static final int PROTOCOL_VERSION = 1;
	/** Marks an {@code applied} or {@code rejected} that answers no client edit (an undo, a redo, a subscribe). */
	public static final int NO_CLIENT_SEQ = -1;
	/** Room for {@link SyncLimits#WIRE_MAX_EDIT_BLOCKS} changes at their largest (a long and a three-byte id each). */
	static final int MAX_EDIT_BYTES = SyncLimits.WIRE_MAX_EDIT_BLOCKS * 12 + 64 * 1024;
	/** Snapshots and applied edits go to players; their size follows the plan's, up to the size cap. */
	private static final int MAX_CLIENTBOUND_BYTES = Integer.MAX_VALUE;
	private static final int MAX_NAME_LENGTH = 256;

	private Payloads() {
	}

	// Client to server.

	public record Subscribe(UUID plan) implements CustomPacketPayload {
		public static final Type<Subscribe> TYPE = payloadType("subscribe");
		static final StreamCodec<FriendlyByteBuf, Subscribe> CODEC = StreamCodec.of(
			(buf, p) -> buf.writeUUID(p.plan), buf -> new Subscribe(buf.readUUID()));

		@Override
		public Type<Subscribe> type() {
			return TYPE;
		}
	}

	public record Unsubscribe(UUID plan) implements CustomPacketPayload {
		public static final Type<Unsubscribe> TYPE = payloadType("unsubscribe");
		static final StreamCodec<FriendlyByteBuf, Unsubscribe> CODEC = StreamCodec.of(
			(buf, p) -> buf.writeUUID(p.plan), buf -> new Unsubscribe(buf.readUUID()));

		@Override
		public Type<Unsubscribe> type() {
			return TYPE;
		}
	}

	public record Edit(UUID plan, int clientSeq, EditKind kind, List<BlockChange> changes) implements CustomPacketPayload {
		public static final Type<Edit> TYPE = payloadType("edit");
		static final StreamCodec<FriendlyByteBuf, Edit> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeUUID(p.plan);
				buf.writeVarInt(p.clientSeq);
				buf.writeEnum(p.kind);
				writeChanges(buf, p.changes);
			},
			buf -> new Edit(buf.readUUID(), buf.readVarInt(), buf.readEnum(EditKind.class), readChanges(buf)));

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

	/** Registered only while {@link OpenChoices#REDO} is on. */
	public record Redo() implements CustomPacketPayload {
		public static final Type<Redo> TYPE = payloadType("redo");
		static final StreamCodec<FriendlyByteBuf, Redo> CODEC = StreamCodec.unit(new Redo());

		@Override
		public Type<Redo> type() {
			return TYPE;
		}
	}

	// Server to client.

	public record Hello(int protocolVersion, SyncLimits limits) implements CustomPacketPayload {
		public static final Type<Hello> TYPE = payloadType("hello");
		static final StreamCodec<FriendlyByteBuf, Hello> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.protocolVersion);
				buf.writeVarInt(p.limits.maxEditBlocks());
				buf.writeVarInt(p.limits.maxPlanSide());
			},
			buf -> new Hello(buf.readVarInt(), new SyncLimits(buf.readVarInt(), buf.readVarInt())));

		@Override
		public Type<Hello> type() {
			return TYPE;
		}
	}

	public record Snapshot(UUID plan, int rev, List<RegionSnapshot> regions) implements CustomPacketPayload {
		public static final Type<Snapshot> TYPE = payloadType("snapshot");
		static final StreamCodec<FriendlyByteBuf, Snapshot> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeUUID(p.plan);
				buf.writeVarInt(p.rev);
				writeList(buf, p.regions, (b, region) -> {
					b.writeUtf(region.name(), MAX_NAME_LENGTH);
					b.writeBlockPos(region.min());
					writeVector(b, region.dimensions());
					writeList(b, region.palette(), Payloads::writeState);
					b.writeLongArray(region.packed());
				});
			},
			buf -> new Snapshot(buf.readUUID(), buf.readVarInt(), readList(buf, b -> new RegionSnapshot(
				b.readUtf(MAX_NAME_LENGTH), b.readBlockPos(), readVector(b), readList(b, Payloads::readState),
				b.readLongArray()))));

		public Snapshot {
			regions = List.copyOf(regions);
		}

		@Override
		public Type<Snapshot> type() {
			return TYPE;
		}
	}

	/** One edit as the server applied it, to every subscriber, its author included. Growth comes before the changes. */
	public record Applied(UUID plan, int rev, UUID author, int clientSeq, List<RegionBox> growth, List<BlockChange> changes)
		implements CustomPacketPayload {
		public static final Type<Applied> TYPE = payloadType("applied");
		static final StreamCodec<FriendlyByteBuf, Applied> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeUUID(p.plan);
				buf.writeVarInt(p.rev);
				buf.writeUUID(p.author);
				buf.writeVarInt(p.clientSeq);
				writeList(buf, p.growth, (b, box) -> {
					b.writeUtf(box.region(), MAX_NAME_LENGTH);
					b.writeBlockPos(box.min());
					writeVector(b, box.dimensions());
				});
				writeChanges(buf, p.changes);
			},
			buf -> new Applied(buf.readUUID(), buf.readVarInt(), buf.readUUID(), buf.readVarInt(),
				readList(buf, b -> new RegionBox(b.readUtf(MAX_NAME_LENGTH), b.readBlockPos(), readVector(b))),
				readChanges(buf)));

		public Applied {
			growth = List.copyOf(growth);
			changes = List.copyOf(changes);
		}

		@Override
		public Type<Applied> type() {
			return TYPE;
		}
	}

	/** The server didn't apply an edit (or a subscribe, with {@link #NO_CLIENT_SEQ}); the client takes a fresh snapshot. */
	public record Rejected(UUID plan, int clientSeq, PlayerMessage reason) implements CustomPacketPayload {
		public static final Type<Rejected> TYPE = payloadType("rejected");
		static final StreamCodec<FriendlyByteBuf, Rejected> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeUUID(p.plan);
				buf.writeVarInt(p.clientSeq);
				writeMessage(buf, p.reason);
			},
			buf -> new Rejected(buf.readUUID(), buf.readVarInt(), readMessage(buf)));

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
		serverbound.register(Subscribe.TYPE, Subscribe.CODEC);
		serverbound.register(Unsubscribe.TYPE, Unsubscribe.CODEC);
		serverbound.registerLarge(Edit.TYPE, Edit.CODEC, MAX_EDIT_BYTES);
		serverbound.register(Undo.TYPE, Undo.CODEC);
		if (OpenChoices.REDO) {
			serverbound.register(Redo.TYPE, Redo.CODEC);
		}
		PayloadTypeRegistry<?> clientbound = PayloadTypeRegistry.clientboundPlay();
		clientbound.register(Hello.TYPE, Hello.CODEC);
		clientbound.registerLarge(Snapshot.TYPE, Snapshot.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.registerLarge(Applied.TYPE, Applied.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.register(Rejected.TYPE, Rejected.CODEC);
		clientbound.register(Notice.TYPE, Notice.CODEC);
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(BuildingPlanner.MOD_ID, path));
	}

	/** Changes go grouped by region, so each region's name travels once. */
	private static void writeChanges(FriendlyByteBuf buf, List<BlockChange> changes) {
		Map<String, List<BlockChange>> byRegion = new LinkedHashMap<>();
		for (BlockChange change : changes) {
			byRegion.computeIfAbsent(change.region(), r -> new ArrayList<>()).add(change);
		}
		writeList(buf, List.copyOf(byRegion.entrySet()), (b, group) -> {
			b.writeUtf(group.getKey(), MAX_NAME_LENGTH);
			writeList(b, group.getValue(), (bb, change) -> {
				bb.writeBlockPos(change.pos());
				writeState(bb, change.state());
			});
		});
	}

	private static List<BlockChange> readChanges(FriendlyByteBuf buf) {
		List<BlockChange> changes = new ArrayList<>();
		int groups = buf.readVarInt();
		for (int g = 0; g < groups; g++) {
			String region = buf.readUtf(MAX_NAME_LENGTH);
			int count = buf.readVarInt();
			for (int i = 0; i < count; i++) {
				changes.add(new BlockChange(region, buf.readBlockPos(), readState(buf)));
			}
		}
		return changes;
	}

	private static void writeState(FriendlyByteBuf buf, BlockState state) {
		buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(state));
	}

	private static BlockState readState(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		BlockState state = Block.BLOCK_STATE_REGISTRY.byId(id);
		if (state == null) {
			throw new IllegalArgumentException("Unknown block state id " + id);
		}
		return state;
	}

	private static void writeMessage(FriendlyByteBuf buf, PlayerMessage message) {
		buf.writeUtf(message.key(), MAX_NAME_LENGTH);
		writeList(buf, message.args(), (b, arg) -> b.writeUtf(arg, MAX_NAME_LENGTH));
	}

	private static PlayerMessage readMessage(FriendlyByteBuf buf) {
		return new PlayerMessage(buf.readUtf(MAX_NAME_LENGTH), readList(buf, b -> b.readUtf(MAX_NAME_LENGTH)));
	}

	private static void writeVector(FriendlyByteBuf buf, Vec3i vector) {
		buf.writeVarInt(vector.getX());
		buf.writeVarInt(vector.getY());
		buf.writeVarInt(vector.getZ());
	}

	private static Vec3i readVector(FriendlyByteBuf buf) {
		return new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
	}

	private interface Writer<T> {
		void write(FriendlyByteBuf buf, T value);
	}

	private static <T> void writeList(FriendlyByteBuf buf, List<T> values, Writer<T> writer) {
		buf.writeVarInt(values.size());
		for (T value : values) {
			writer.write(buf, value);
		}
	}

	/** Doesn't trust the count for the list's capacity: a short buffer runs out before a false count does harm. */
	private static <T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		int count = buf.readVarInt();
		List<T> values = new ArrayList<>(Math.min(Math.max(count, 0), 1024));
		for (int i = 0; i < count; i++) {
			values.add(reader.apply(buf));
		}
		return values;
	}
}
