package dev.buildingplanner.server;

import dev.buildingplanner.BuildingPlanner;
import dev.buildingplanner.input.RealPlace;
import dev.buildingplanner.input.RealPlace.Entry;
import dev.buildingplanner.input.RealPlace.Tried;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Applies a player's {@code place} (see "Place real blocks"): checks its blocks as
 * {@code ServerGamePacketListenerImpl.handleUseItemOn} checks a right click, then places each block that goes in as the
 * player's own right click would place it there, through Minecraft's {@code BlockItem.place}, in
 * {@link RealPlace#order}, from the player's inventory, while hooks S2 and S3 read the block it places now. One per
 * running server ({@link ServerSync}), used on the server thread.
 */
public final class RealPlacer {
	/** The {@code place} the server applies now, which hooks S2 and S3 read, or null. Only the server thread sets it. */
	private static Placing placing;

	private final PlaceRate rate = new PlaceRate(RealPlace.MAX_BLOCKS);

	RealPlacer() {
	}

	/** A {@code place} underway: its player, whether it's a paste, the block placed now, and what S2 and S3 noted. */
	private static final class Placing {
		final ServerPlayer player;
		final boolean paste;
		/** The block {@code BlockItem.place} places now, or null between blocks. */
		Entry block;
		/** S2 found that the block couldn't stay there, or that an entity was in its way. */
		boolean cantStay;
		/** A block of this {@code place} played its place sound. */
		boolean sounded;

		Placing(ServerPlayer player, boolean paste) {
			this.player = player;
			this.paste = paste;
		}
	}

	/** Hook S2: whether the server places a shape's or a paste's real block for {@code player} at {@code pos} now. */
	public static boolean places(Player player, BlockPos pos) {
		Placing now = placing;
		return now != null && now.block != null && now.player == player && now.block.pos().equals(pos);
	}

	/**
	 * Hook S2's state for the block {@link #places} places: {@code placed}, as Minecraft gives it for the context, in the
	 * shape's form, orientation and count, and for a paste a door's hinge ({@link RealPlace#formed}), checked as
	 * Disable's turn is ({@link PlaceTurns#checked}, with {@code placeable} Minecraft's own check), or null where it
	 * doesn't go in.
	 */
	public static BlockState state(BlockPlaceContext context, BlockState placed, Predicate<BlockState> placeable) {
		Placing now = placing;
		BlockState formed = RealPlace.formed(placed, now.block.state(), now.paste);
		if (formed == null) {
			// Minecraft gave no state where its own block can't stay or an entity is in its way.
			now.cantStay = placed == null;
			return null;
		}
		PlaceTurns.Placed checked = PlaceTurns.checked(context, placed, formed, placeable);
		if (!checked.stays()) {
			now.cantStay = true;
			return null;
		}
		return checked.state();
	}

	/**
	 * Hook S3: whether {@code BlockItem.place} plays its place sound to the players near {@code entity}: always, but
	 * while the server places a shape or a paste, only for its first block placed, which for a paste this plays to that
	 * player too, since their game plays none for a paste.
	 */
	public static boolean playsSound(Level level, Entity entity, BlockPos pos, SoundEvent sound, SoundSource source,
		float volume, float pitch) {
		Placing now = placing;
		if (now == null || now.block == null || entity != now.player) {
			return true;
		}
		if (now.sounded) {
			return false;
		}
		now.sounded = true;
		if (now.paste) {
			level.playSound(null, pos, sound, source, volume, pitch);
			return false;
		}
		return true;
	}

	/** Applies the player's {@code place}: places what goes in, or nothing, and sends a {@code notice} where it says to. */
	void place(ServerPlayer player, Payloads.Place place) {
		int sent = place.blocks().size();
		if (RealPlace.tooMany(sent)) {
			notice(player, PlayerMessage.placeTooMany(sent));
			return;
		}
		MinecraftServer server = player.level().getServer();
		ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, place.dimension()));
		if (level == null) {
			return;
		}
		List<Entry> blocks = RealPlace.withoutSecondHalves(
			place.blocks().stream().map(change -> new Entry(change.pos(), change.state())).toList());
		Vec3 eyes = player.getEyePosition();
		boolean mayPlace = level == player.level() && player.mayBuild();
		List<Entry> going = RealPlace.goingIn(blocks, world(level, player, mayPlace), eyes);
		Map<Item, Integer> lacking = RealPlace.Inventory.of(player).shortOf(RealPlace.needs(going));
		if (!lacking.isEmpty()) {
			notice(player, PlayerMessage.placeShort(names(lacking)));
			return;
		}
		long start = Util.getMillis();
		if (!rate.allows(player.getUUID(), going.size(), start)) {
			notice(player, PlayerMessage.placeTooFast());
			return;
		}
		Map<Entry, Tried> lastTry = new IdentityHashMap<>();
		Placing now = new Placing(player, place.paste());
		placing = now;
		List<Entry> placed;
		try {
			placed = RealPlace.order(going, block -> {
				Tried tried = tryPlacing(level, player, block, place, now);
				lastTry.put(block, tried);
				return tried;
			});
		} finally {
			placing = null;
		}
		long end = Util.getMillis();
		rate.placed(player.getUUID(), placed.size(), end);
		// At debug level, so the in-game checks can read the time a place takes (the dev server's debug.log).
		BuildingPlanner.LOGGER.debug("Building Planner placed {} of {}'s {} real blocks in {} ms", placed.size(),
			player.getGameProfile().name(), sent, end - start);
		// The blocks the player's game showed going in that the server left out: those its own checks keep out (an
		// unloaded chunk, spawn protection, the game mode, another dimension), and those Minecraft refused for a reason
		// other than the ones the player's game checks too, a block that can't stay or an entity in the way.
		int seen = RealPlace.goingIn(blocks, seen(level, player), eyes).size();
		int refused = (int) lastTry.values().stream().filter(tried -> tried == Tried.LEFT_OUT).count();
		int leftOut = seen - going.size() + refused;
		if (leftOut > 0) {
			notice(player, PlayerMessage.placeLeftOut(leftOut));
		}
	}

	/** The player left: their count for the cap a second goes. */
	void left(UUID player) {
		rate.left(player);
	}

	/** Places {@code block} as the player's right click on it with its item would, with the shape's face and hit. */
	private static Tried tryPlacing(ServerLevel level, ServerPlayer player, Entry block, Payloads.Place place,
		Placing now) {
		BlockPos pos = block.pos();
		// An earlier block of the same place may have filled the space since the checks, as a bed's head does.
		if (!RealPlace.spaceTakes(level.getBlockState(pos), block.state())) {
			return Tried.LEFT_OUT;
		}
		Item item = RealPlace.item(block.state());
		ItemStack stack = stack(player, item);
		if (stack == null) {
			return Tried.LEFT_OUT;
		}
		// The face and hit of the shape's first click, at the block's own height (EffortlessShapes.states).
		Vec3 hit = place.hit();
		double yFraction = hit.y - Math.floor(hit.y);
		BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
			new BlockHitResult(new Vec3(hit.x, pos.getY() + yFraction, hit.z), place.face(), pos, false));
		if (!context.getClickedPos().equals(pos)) {
			return Tried.LEFT_OUT;
		}
		// Scaffolding with nothing to hold it up gets no place from its item (ScaffoldingBlockItem), before hook S2.
		if (context.canPlace() && ((BlockItem) item).updatePlacementContext(context) == null) {
			return Tried.CANT_STAY;
		}
		now.block = block;
		now.cantStay = false;
		InteractionResult result;
		try {
			result = ((BlockItem) item).place(context);
		} finally {
			now.block = null;
		}
		if (!result.consumesAction()) {
			return now.cantStay ? Tried.CANT_STAY : Tried.LEFT_OUT;
		}
		// BlockItem.place used up one item; a block made of several uses the rest.
		for (RealPlace.Take take : RealPlace.Inventory.of(player).takes(item, RealPlace.items(block.state()) - 1)) {
			player.getInventory().getItem(take.slot()).shrink(take.count());
		}
		return Tried.PLACED;
	}

	/**
	 * The stack a block of {@code item} places from: in creative a stack the server makes, and otherwise the first the
	 * inventory takes it from ({@link RealPlace.Inventory#takes}), or null with none left.
	 */
	private static ItemStack stack(ServerPlayer player, Item item) {
		if (player.hasInfiniteMaterials()) {
			return new ItemStack(item);
		}
		List<RealPlace.Take> takes = RealPlace.Inventory.of(player).takes(item, 1);
		return takes.isEmpty() ? null : player.getInventory().getItem(takes.getFirst().slot());
	}

	/** Each item short and how many, by its English name as the server gives it, as "1 oak planks, 4 mossy cobblestone". */
	private static String names(Map<Item, Integer> lacking) {
		return PlayerMessage.missing(lacking.entrySet().stream()
			.map(lack -> Map.entry(lack.getKey().getName(new ItemStack(lack.getKey())).getString(), lack.getValue()))
			.toList());
	}

	/**
	 * The world as the server's checks before placing see it ({@code handleUseItemOn}'s): nothing while the player may
	 * not place there ({@code mayPlace}), and otherwise a block in a loaded chunk, within the build height, inside the
	 * world border and outside spawn protection.
	 */
	private static RealPlace.World world(ServerLevel level, ServerPlayer player, boolean mayPlace) {
		MinecraftServer server = level.getServer();
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return mayPlace && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
					&& loaded(level, pos) && !server.isUnderSpawnProtection(level, pos, player)
					&& level.mayInteract(player, pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return level.getBlockState(pos);
			}

			@Override
			public boolean unobstructed(Entry block) {
				return level.isUnobstructed(block.state(), block.pos(), CollisionContext.placementContext(player));
			}
		};
	}

	/**
	 * The world as the player's own game checks it, by which the notice counts the blocks left out: within the build
	 * height and inside the world border, and where the server hasn't loaded the chunk, a space that takes the block
	 * with nothing in the way.
	 */
	private static RealPlace.World seen(ServerLevel level, ServerPlayer player) {
		return new RealPlace.World() {
			@Override
			public boolean allows(BlockPos pos) {
				return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
			}

			@Override
			public BlockState at(BlockPos pos) {
				return loaded(level, pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
			}

			@Override
			public boolean unobstructed(Entry block) {
				return !loaded(level, block.pos())
					|| level.isUnobstructed(block.state(), block.pos(), CollisionContext.placementContext(player));
			}
		};
	}

	/** Whether the server has the chunk holding {@code pos} loaded, which placing never loads. */
	private static boolean loaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getZ())) != null;
	}

	private static void notice(ServerPlayer player, PlayerMessage message) {
		if (ServerPlayNetworking.canSend(player, Payloads.Notice.TYPE)) {
			ServerPlayNetworking.send(player, new Payloads.Notice(message));
		}
	}
}
