package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** "Match ghost blocks": the setting's saved value, and the ghost block's turn a right click in Disable takes. */
class GhostMatchTest {
	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static BlockState oakStairs(Direction facing) {
		return Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	private static BlockState wallTorch(Direction facing) {
		return Blocks.WALL_TORCH.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
	}

	@Test
	void aStairPlacedFacingSouthIntoAGhostStairFacingEastTakesTheGhostStairsTurn() {
		BlockTurn.Held turn = GhostMatch.turn(true, oakStairs(Direction.EAST), oakStairs(Direction.SOUTH), Items.OAK_STAIRS);
		assertSame(Items.OAK_STAIRS, turn.item());
		assertEquals(Map.of("facing", "east", "half", "bottom"), turn.turn().values());
		BlockState placed = PlaceTurn.turned(oakStairs(Direction.SOUTH), turn, Items.OAK_STAIRS, true);
		assertEquals(Direction.EAST, placed.getValue(BlockStateProperties.HORIZONTAL_FACING));
	}

	@Test
	void withTheSettingOffTheStairFacesAsThePlayerDoes() {
		assertSame(BlockTurn.Held.NONE,
			GhostMatch.turn(false, oakStairs(Direction.EAST), oakStairs(Direction.SOUTH), Items.OAK_STAIRS));
	}

	@Test
	void aDifferentBlockOrNoGhostBlockTakesNoTurn() {
		BlockState spruce = Blocks.SPRUCE_STAIRS.defaultBlockState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		assertSame(BlockTurn.Held.NONE, GhostMatch.turn(true, spruce, oakStairs(Direction.SOUTH), Items.OAK_STAIRS));
		assertSame(BlockTurn.Held.NONE,
			GhostMatch.turn(true, Blocks.AIR.defaultBlockState(), oakStairs(Direction.SOUTH), Items.OAK_STAIRS));
		assertSame(BlockTurn.Held.NONE, GhostMatch.turn(true, null, oakStairs(Direction.SOUTH), Items.OAK_STAIRS));
		// Minecraft's own right click places nothing there.
		assertSame(BlockTurn.Held.NONE, GhostMatch.turn(true, oakStairs(Direction.EAST), null, Items.OAK_STAIRS));
		// A ghost block with no orientation property has no turn to give.
		assertSame(BlockTurn.Held.NONE,
			GhostMatch.turn(true, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState(), Items.STONE));
	}

	@Test
	void aTorchPlacedOnAWallMatchesAGhostWallTorchAndNotAGhostTorch() {
		BlockTurn.Held turn = GhostMatch.turn(true, wallTorch(Direction.EAST), wallTorch(Direction.NORTH), Items.TORCH);
		assertSame(Items.TORCH, turn.item());
		assertEquals(Map.of("facing", "east"), turn.turn().values());
		assertSame(BlockTurn.Held.NONE,
			GhostMatch.turn(true, Blocks.TORCH.defaultBlockState(), wallTorch(Direction.NORTH), Items.TORCH));
	}

	@Test
	void aTurnThatCantStayLeavesTheClickAsItIsWithTheSettingOff() {
		BlockTurn.Held ghost = GhostMatch.turn(true, wallTorch(Direction.EAST), wallTorch(Direction.NORTH), Items.TORCH);
		BlockTurn.Held leftCtrl = PlaceTurn.accepted(Items.TORCH, Map.of("facing", "west"));
		assertSame(leftCtrl, GhostMatch.clickTakes(ghost, false, leftCtrl));
		assertSame(BlockTurn.Held.NONE, GhostMatch.clickTakes(ghost, false, BlockTurn.Held.NONE));
		// Where it can stay, the ghost block's turn takes the place of Left Ctrl's.
		assertSame(ghost, GhostMatch.clickTakes(ghost, true, leftCtrl));
		assertSame(leftCtrl, GhostMatch.clickTakes(BlockTurn.Held.NONE, true, leftCtrl));
	}

	@Test
	void theSettingIsOnOnlyAsSavedOnAndStartsOff() {
		assertTrue(GhostMatch.on(GhostMatch.saved(true)));
		assertFalse(GhostMatch.on(GhostMatch.saved(false)));
		assertFalse(GhostMatch.on(null));
		assertFalse(GhostMatch.on("yes"));
		assertEquals("true", GhostMatch.saved(true));
	}
}
