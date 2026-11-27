package dev.buildingplanner.sync;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.layer.PackedSection;
import java.util.ArrayList;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Building Planner's network payloads for the shared layer, the material lists, the turn of real blocks placed in
 * Disable and the real blocks of a shape or a paste, namespace {@code buildingplanner},
 * protocol version 5. Dimensions travel as their ids; block states as ids in the block-state registry and items as ids
 * in the item registry, since both sides run the same vanilla blocks and items.
 */
public final class Payloads {
	/** Raised whenever a payload's layout changes; a client that sees another version turns Building Planner off. */
	public static final int PROTOCOL_VERSION = 5;
	/** Marks an {@code applied} that answers no edit of the player's: an undo, a redo, or ghost blocks real ones filled. */
	public static final int NO_CLIENT_SEQ = -1;
	/** The author of an {@code applied} that removes ghost blocks real blocks filled, which games draw without a glow. */
	public static final UUID FILLED_AUTHOR = new UUID(0L, 0L);
	/** Room for {@link SyncLimits#WIRE_MAX_EDIT_BLOCKS} changes at their largest (a long and a three-byte id each). */
	static final int MAX_EDIT_BYTES = SyncLimits.WIRE_MAX_EDIT_BLOCKS * 12 + 64 * 1024;
	/** Room for 131,072 columns in one {@code watch} or {@code unwatch}, far more than the largest view distance loads. */
	static final int MAX_WATCH_BYTES = 1024 * 1024;
	/** Column snapshots and applied edits go to players; their size follows the layer's. */
	private static final int MAX_CLIENTBOUND_BYTES = Integer.MAX_VALUE;
	private static final int MAX_NAME_LENGTH = 256;
	/**
	 * The most characters a list's name may have on the wire, past {@link ListLimits#MAX_NAME_LENGTH}, so that a longer
	 * name reaches the server's check, which drops it with a warning, rather than ending the connection.
	 */
	private static final int MAX_WIRE_LIST_NAME = 1024;
	/** Room for a list of {@link ListLimits#MAX_LIST_BYTES} and the rest of its message. */
	static final int MAX_LIST_MESSAGE_BYTES = ListLimits.MAX_LIST_BYTES + 8 * 1024;

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

	/**
	 * Keep the list under the name, replacing a kept list of that name. {@code tooLarge} is set on reading a list over
	 * {@link ListLimits#MAX_LIST_BYTES} as sent, whose contents are then skipped, and {@code list} is then null.
	 */
	public record SaveList(String name, ListData list, boolean tooLarge) implements CustomPacketPayload {
		public static final Type<SaveList> TYPE = payloadType("save_list");
		static final StreamCodec<FriendlyByteBuf, SaveList> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeUtf(p.name, MAX_WIRE_LIST_NAME);
				writeSizedList(buf, p.list);
			},
			buf -> {
				String name = buf.readUtf(MAX_WIRE_LIST_NAME);
				ListData list = readSizedList(buf);
				return new SaveList(name, list, list == null);
			});

		public SaveList(String name, ListData list) {
			this(name, list, false);
		}

		@Override
		public Type<SaveList> type() {
			return TYPE;
		}
	}

	/** Delete the kept list of that name. */
	public record DeleteList(String name) implements CustomPacketPayload {
		public static final Type<DeleteList> TYPE = payloadType("delete_list");
		static final StreamCodec<FriendlyByteBuf, DeleteList> CODEC = StreamCodec.of(
			(buf, p) -> buf.writeUtf(p.name, MAX_WIRE_LIST_NAME), buf -> new DeleteList(buf.readUtf(MAX_WIRE_LIST_NAME)));

		@Override
		public Type<DeleteList> type() {
			return TYPE;
		}
	}

	/** Send the kept list of that name, or "Last counted" when the name is empty. */
	public record OpenList(String name) implements CustomPacketPayload {
		public static final Type<OpenList> TYPE = payloadType("open_list");
		static final StreamCodec<FriendlyByteBuf, OpenList> CODEC = StreamCodec.of(
			(buf, p) -> buf.writeUtf(p.name, MAX_WIRE_LIST_NAME), buf -> new OpenList(buf.readUtf(MAX_WIRE_LIST_NAME)));

		@Override
		public Type<OpenList> type() {
			return TYPE;
		}
	}

	/**
	 * The current list, or none; whether it was just counted from a box, which makes it "Last counted" too; and whether
	 * the panel is on. {@code tooLarge} is as for {@link SaveList}, with {@code list} then empty.
	 */
	public record CurrentList(Optional<ListData> list, boolean counted, boolean panel, boolean tooLarge)
		implements CustomPacketPayload {
		public static final Type<CurrentList> TYPE = payloadType("current_list");
		static final StreamCodec<FriendlyByteBuf, CurrentList> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeBoolean(p.list.isPresent());
				p.list.ifPresent(list -> writeSizedList(buf, list));
				buf.writeBoolean(p.counted);
				buf.writeBoolean(p.panel);
			},
			buf -> {
				boolean present = buf.readBoolean();
				ListData list = present ? readSizedList(buf) : null;
				return new CurrentList(Optional.ofNullable(list), buf.readBoolean(), buf.readBoolean(),
					present && list == null);
			});

		public CurrentList(Optional<ListData> list, boolean counted, boolean panel) {
			this(list, counted, panel, false);
		}

		@Override
		public Type<CurrentList> type() {
			return TYPE;
		}
	}

	/**
	 * The turn of the real block a right click places in Disable (see "Turning real blocks in Disable"): the item it's
	 * for and its orientation properties' values by name, or no item to clear it.
	 */
	public record PlaceTurn(Optional<Item> item, Map<String, String> properties) implements CustomPacketPayload {
		public static final Type<PlaceTurn> TYPE = payloadType("place_turn");
		/** More properties than a turn has ({@code BlockTurn}'s orientation properties) end the connection. */
		private static final int MAX_PROPERTIES = 16;
		static final StreamCodec<FriendlyByteBuf, PlaceTurn> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeBoolean(p.item.isPresent());
				p.item.ifPresent(item -> buf.writeVarInt(BuiltInRegistries.ITEM.getId(item)));
				buf.writeVarInt(p.properties.size());
				p.properties.forEach((name, value) -> {
					buf.writeUtf(name, dev.buildingplanner.input.PlaceTurn.MAX_TEXT);
					buf.writeUtf(value, dev.buildingplanner.input.PlaceTurn.MAX_TEXT);
				});
			},
			buf -> {
				Optional<Item> item = buf.readBoolean() ? Optional.of(readItem(buf)) : Optional.empty();
				int count = buf.readVarInt();
				if (count < 0 || count > MAX_PROPERTIES) {
					throw new IllegalArgumentException("A turn of " + count + " properties");
				}
				Map<String, String> properties = new TreeMap<>();
				for (int i = 0; i < count; i++) {
					properties.put(buf.readUtf(dev.buildingplanner.input.PlaceTurn.MAX_TEXT),
						buf.readUtf(dev.buildingplanner.input.PlaceTurn.MAX_TEXT));
				}
				return new PlaceTurn(item, properties);
			});

		public PlaceTurn {
			properties = Map.copyOf(properties);
		}

		@Override
		public Type<PlaceTurn> type() {
			return TYPE;
		}
	}

	/**
	 * The real blocks of a shape or a paste while "Place real blocks" acts (see "Place real blocks"): the first click's
	 * face and hit, each block's position and state as the preview shows it, and whether it's a paste, whose first block
	 * plays its sound to the player too. A hit that isn't a finite position ends the connection, as Minecraft's own
	 * check of a move does.
	 */
	public record Place(Identifier dimension, Direction face, Vec3 hit, boolean paste, List<LayerChange> blocks)
		implements CustomPacketPayload {
		public static final Type<Place> TYPE = payloadType("place");
		static final StreamCodec<FriendlyByteBuf, Place> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeIdentifier(p.dimension);
				buf.writeEnum(p.face);
				buf.writeDouble(p.hit.x);
				buf.writeDouble(p.hit.y);
				buf.writeDouble(p.hit.z);
				buf.writeBoolean(p.paste);
				writeChanges(buf, p.blocks);
			},
			buf -> {
				Identifier dimension = buf.readIdentifier();
				Direction face = buf.readEnum(Direction.class);
				Vec3 hit = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
				if (!Double.isFinite(hit.x) || !Double.isFinite(hit.y) || !Double.isFinite(hit.z)) {
					throw new IllegalArgumentException("A place's hit isn't a position: " + hit);
				}
				return new Place(dimension, face, hit, buf.readBoolean(), readChanges(buf));
			});

		public Place {
			blocks = List.copyOf(blocks);
		}

		@Override
		public Type<Place> type() {
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
	 * touches, its author included. Ghost blocks real blocks filled leave as an edit by {@link #FILLED_AUTHOR}.
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

	/**
	 * The index of the player's lists: "Last counted", when there is one, and the saved lists, newest first. It goes at
	 * join and after each save and delete.
	 */
	public record Lists(Optional<ListSummary> lastCounted, List<ListSummary> saved) implements CustomPacketPayload {
		public static final Type<Lists> TYPE = payloadType("lists");
		static final StreamCodec<FriendlyByteBuf, Lists> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeBoolean(p.lastCounted.isPresent());
				p.lastCounted.ifPresent(summary -> writeSummary(buf, summary));
				writeList(buf, p.saved, Payloads::writeSummary);
			},
			buf -> new Lists(buf.readBoolean() ? Optional.of(readSummary(buf)) : Optional.empty(),
				readList(buf, Payloads::readSummary)));

		public Lists {
			saved = List.copyOf(saved);
		}

		@Override
		public Type<Lists> type() {
			return TYPE;
		}
	}

	/** One list in a {@link Lists}: its name (empty for "Last counted"), its time and its box's size. */
	public record ListSummary(String name, long time, Vec3i size) {
		public static ListSummary of(ListData list) {
			return new ListSummary(list.name().orElse(""), list.time().orElse(0), list.size());
		}
	}

	/** The answer to {@code open_list}. */
	public record OpenedList(ListData list) implements CustomPacketPayload {
		public static final Type<OpenedList> TYPE = payloadType("list");
		static final StreamCodec<FriendlyByteBuf, OpenedList> CODEC = StreamCodec.of(
			(buf, p) -> writeSizedList(buf, p.list), buf -> new OpenedList(requireList(readSizedList(buf))));

		@Override
		public Type<OpenedList> type() {
			return TYPE;
		}
	}

	/** At join: the current list as the player left it, or none, and the panel's switch. */
	public record Current(Optional<ListData> list, boolean panel) implements CustomPacketPayload {
		public static final Type<Current> TYPE = payloadType("current");
		static final StreamCodec<FriendlyByteBuf, Current> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeBoolean(p.list.isPresent());
				p.list.ifPresent(list -> writeSizedList(buf, list));
				buf.writeBoolean(p.panel);
			},
			buf -> new Current(buf.readBoolean() ? Optional.of(requireList(readSizedList(buf))) : Optional.empty(),
				buf.readBoolean()));

		@Override
		public Type<Current> type() {
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
		serverbound.registerLarge(SaveList.TYPE, SaveList.CODEC, MAX_LIST_MESSAGE_BYTES);
		serverbound.register(DeleteList.TYPE, DeleteList.CODEC);
		serverbound.register(OpenList.TYPE, OpenList.CODEC);
		serverbound.registerLarge(CurrentList.TYPE, CurrentList.CODEC, MAX_LIST_MESSAGE_BYTES);
		serverbound.register(PlaceTurn.TYPE, PlaceTurn.CODEC);
		// As large as an edit, so a shape past the cap of real blocks arrives and gets the cap's notice.
		serverbound.registerLarge(Place.TYPE, Place.CODEC, MAX_EDIT_BYTES);
		PayloadTypeRegistry<?> clientbound = PayloadTypeRegistry.clientboundPlay();
		clientbound.register(Hello.TYPE, Hello.CODEC);
		clientbound.registerLarge(Columns.TYPE, Columns.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.registerLarge(Applied.TYPE, Applied.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.register(Rejected.TYPE, Rejected.CODEC);
		clientbound.register(Notice.TYPE, Notice.CODEC);
		// The index grows with the player's lists, which have no limit; a single list is far below the 1 MiB a
		// clientbound payload may carry.
		clientbound.registerLarge(Lists.TYPE, Lists.CODEC, MAX_CLIENTBOUND_BYTES);
		clientbound.register(OpenedList.TYPE, OpenedList.CODEC);
		clientbound.register(Current.TYPE, Current.CODEC);
	}

	/** How many bytes a list takes as sent, which {@link ListLimits#MAX_LIST_BYTES} limits. */
	public static int listBytes(ListData list) {
		FriendlyByteBuf body = new FriendlyByteBuf(Unpooled.buffer());
		writeListBody(body, list);
		return body.readableBytes();
	}

	/** A list as its size in bytes, then its contents, so that a reader can skip one over the limit. */
	static void writeSizedList(FriendlyByteBuf buf, ListData list) {
		FriendlyByteBuf body = new FriendlyByteBuf(Unpooled.buffer());
		writeListBody(body, list);
		buf.writeVarInt(body.readableBytes());
		buf.writeBytes(body);
	}

	/** The list {@link #writeSizedList} wrote, or null, with its contents skipped, when it's over the limit. */
	static ListData readSizedList(FriendlyByteBuf buf) {
		int bytes = buf.readVarInt();
		if (bytes < 0) {
			throw new IllegalArgumentException("A list of " + bytes + " bytes");
		}
		if (bytes > ListLimits.MAX_LIST_BYTES) {
			buf.skipBytes(bytes);
			return null;
		}
		FriendlyByteBuf body = new FriendlyByteBuf(buf.readSlice(bytes));
		ListData list = readListBody(body);
		if (body.isReadable()) {
			throw new IllegalArgumentException("A list's contents are shorter than its size");
		}
		return list;
	}

	private static ListData requireList(ListData list) {
		if (list == null) {
			throw new IllegalArgumentException("A list over " + ListLimits.MAX_LIST_BYTES + " bytes");
		}
		return list;
	}

	private static void writeListBody(FriendlyByteBuf buf, ListData list) {
		buf.writeBoolean(list.name().isPresent());
		list.name().ifPresent(name -> buf.writeUtf(name, MAX_WIRE_LIST_NAME));
		buf.writeBoolean(list.time().isPresent());
		list.time().ifPresent(buf::writeLong);
		buf.writeIdentifier(list.dimension());
		buf.writeBlockPos(list.min());
		buf.writeBlockPos(list.max());
		buf.writeVarInt(list.multiplier());
		buf.writeEnum(list.sort());
		buf.writeBoolean(list.reverse());
		buf.writeBoolean(list.hideAvailable());
		writeList(buf, list.rows(), (b, row) -> {
			b.writeVarInt(BuiltInRegistries.ITEM.getId(row.item()));
			b.writeVarInt(row.total());
			b.writeVarInt(row.missing());
			b.writeVarInt(row.wrong());
		});
	}

	private static ListData readListBody(FriendlyByteBuf buf) {
		Optional<String> name = buf.readBoolean() ? Optional.of(buf.readUtf(MAX_WIRE_LIST_NAME)) : Optional.empty();
		OptionalLong time = buf.readBoolean() ? OptionalLong.of(buf.readLong()) : OptionalLong.empty();
		return new ListData(name, time, buf.readIdentifier(), buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(),
			buf.readEnum(ListSort.class), buf.readBoolean(), buf.readBoolean(),
			readList(buf, b -> new ListData.Row(readItem(b), b.readVarInt(), b.readVarInt(), b.readVarInt())));
	}

	private static Item readItem(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		Item item = BuiltInRegistries.ITEM.byId(id);
		if (item == null || BuiltInRegistries.ITEM.getId(item) != id) {
			throw new IllegalArgumentException("Unknown item id " + id);
		}
		return item;
	}

	private static void writeSummary(FriendlyByteBuf buf, ListSummary summary) {
		buf.writeUtf(summary.name(), MAX_WIRE_LIST_NAME);
		buf.writeLong(summary.time());
		buf.writeVarInt(summary.size().getX());
		buf.writeVarInt(summary.size().getY());
		buf.writeVarInt(summary.size().getZ());
	}

	private static ListSummary readSummary(FriendlyByteBuf buf) {
		return new ListSummary(buf.readUtf(MAX_WIRE_LIST_NAME), buf.readLong(),
			new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
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
