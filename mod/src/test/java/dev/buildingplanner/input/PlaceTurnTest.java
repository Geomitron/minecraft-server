package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The turn hook S2 gives a real block placed in Disable: which turns it takes, and when the turned block can stay. */
class PlaceTurnTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static BlockState stairNorth() {
		return Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
	}

	private static BlockTurn.Held stairsEast() {
		return PlaceTurn.accepted(Items.OAK_STAIRS, Map.of("facing", "east", "half", "bottom"));
	}

	@Test
	void aTurnForTheItemInTheMainHandTurnsTheBlock() {
		BlockState turned = PlaceTurn.turned(stairNorth(), stairsEast(), Items.OAK_STAIRS, true);
		assertEquals(Direction.EAST, turned.getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	void anotherItemOrTheOffHandKeepsMinecraftsState() {
		assertSame(stairNorth(), PlaceTurn.turned(stairNorth(), stairsEast(), Items.SPRUCE_STAIRS, true));
		assertSame(stairNorth(), PlaceTurn.turned(stairNorth(), stairsEast(), Items.OAK_STAIRS, false));
	}

	@Test
	void aPlayerWithoutATurnPlacesAsMinecraftDoes() {
		assertSame(stairNorth(), PlaceTurn.turned(stairNorth(), BlockTurn.Held.NONE, Items.OAK_STAIRS, true));
		assertNull(PlaceTurn.turned(null, stairsEast(), Items.OAK_STAIRS, true));
	}

	@Test
	void noItemClearsTheTurn() {
		assertSame(BlockTurn.Held.NONE, PlaceTurn.accepted(null, Map.of("facing", "east")));
	}

	@Test
	void propertiesOutsideTheTurnsAreRefused() {
		BlockTurn.Held turn = PlaceTurn.accepted(Items.OAK_STAIRS,
			Map.of("facing", "east", "waterlogged", "true", "shape", "outer_left"));
		assertEquals(Map.of("facing", "east"), turn.turn().values());
		assertSame(BlockTurn.Held.NONE, PlaceTurn.accepted(Items.OAK_STAIRS, Map.of("waterlogged", "true")));
		BlockState turned = PlaceTurn.turned(stairNorth(), turn, Items.OAK_STAIRS, true);
		assertFalse(turned.getValue(BlockStateProperties.WATERLOGGED));
	}

	@Test
	void valuesTheBlockDoesntHaveAreRefused() {
		// A stair faces only sideways, and has no axis.
		BlockTurn.Held turn = PlaceTurn.accepted(Items.OAK_STAIRS, Map.of("facing", "up", "axis", "x", "half", "dusk"));
		assertSame(stairNorth(), PlaceTurn.turned(stairNorth(), turn, Items.OAK_STAIRS, true));
	}

	@Test
	void aTwoPartBlockKeepsItsShapeAndAnotherIsReshaped() {
		assertTrue(PlaceTurn.reshapes(stairNorth()));
		assertFalse(PlaceTurn.reshapes(Blocks.OAK_DOOR.defaultBlockState()));
		assertFalse(PlaceTurn.reshapes(redBedFoot(Direction.NORTH)));
		assertFalse(PlaceTurn.reshapes(Blocks.TALL_GRASS.defaultBlockState()));
	}

	/** A red bed's foot facing that way, as Minecraft places it first. */
	private static BlockState redBedFoot(Direction facing) {
		return Blocks.BED.pick(DyeColor.RED).defaultBlockState().setValue(BlockStateProperties.BED_PART, BedPart.FOOT)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	@Test
	void aBedsHeadGoesTowardItsTurnedFacing() {
		Item bed = Items.BED.pick(DyeColor.RED);
		BlockState turned = PlaceTurn.turned(redBedFoot(Direction.NORTH),
			PlaceTurn.accepted(bed, Map.of("facing", "east")), bed, true);
		assertEquals(Direction.EAST, PlaceTurn.otherPart(turned));
		assertNull(PlaceTurn.otherPart(stairNorth()));
	}

	@Test
	void aBedStaysOnlyWhereTheSpaceTowardItsFacingTakesABlock() {
		BlockState bed = redBedFoot(Direction.EAST);
		assertTrue(PlaceTurn.canStay(bed, true, (state, toward) -> toward == Direction.EAST));
		assertFalse(PlaceTurn.canStay(bed, true, (state, toward) -> toward != Direction.EAST));
		assertFalse(PlaceTurn.canStay(bed, false, (state, toward) -> true));
	}

	@Test
	void anotherBlockStaysWhereMinecraftsCheckLetsIt() {
		assertTrue(PlaceTurn.canStay(stairNorth(), true, (state, toward) -> false));
		assertFalse(PlaceTurn.canStay(Blocks.WALL_TORCH.defaultBlockState(), false, (state, toward) -> true));
	}
}
