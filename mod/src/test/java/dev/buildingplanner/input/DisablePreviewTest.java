package dev.buildingplanner.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Disable's preview: when it shows, the reach the server places within, and the blocks that hang on the one behind. */
class DisablePreviewTest {
	/** A player's eyes standing at 0.5 0 0.5, and the survival block interaction range. */
	private static final Vec3 EYE = new Vec3(0.5, 1.62, 0.5);
	private static final double RANGE = 4.5;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void showsWhileTheControlsAreActiveTheSprintKeyIsHeldAndTheClickReachesTheWorld() {
		assertTrue(DisablePreview.shows(true, true, false));
		assertFalse(DisablePreview.shows(false, true, false));
		assertFalse(DisablePreview.shows(true, false, false));
		assertFalse(DisablePreview.shows(true, true, true));
	}

	@Test
	void freecamKeepsTheClickFromTheWorldOnlyWithItsCameraOnPlayerControlOffAndInteractionOff() {
		assertTrue(DisablePreview.interactionBlocked(true, false, false));
		assertFalse(DisablePreview.interactionBlocked(true, false, true));
		assertFalse(DisablePreview.interactionBlocked(true, true, false));
		assertFalse(DisablePreview.interactionBlocked(false, false, false));
	}

	@Test
	void theServerPlacesWithinTheRangeAndABlockOfThePlayersEyesNotTheCamerasWherever() {
		assertTrue(DisablePreview.withinReach(EYE, new BlockPos(0, 0, 5), RANGE));
		// The block's nearest point 4.74 away: past the range, within its extra block.
		assertTrue(DisablePreview.withinReach(EYE, new BlockPos(2, 1, 5), RANGE));
		assertFalse(DisablePreview.withinReach(EYE, new BlockPos(0, 1, 7), RANGE));
		// Freecam's camera 3 blocks from a block 12 blocks from the player: the player's eyes decide.
		assertFalse(DisablePreview.withinReach(EYE, new BlockPos(12, 0, 0), RANGE));
		assertTrue(DisablePreview.withinReach(EYE, new BlockPos(12, 0, 0), 12));
	}

	@Test
	void blocksThatHangOnTheBlockBehindThem() {
		assertTrue(DisablePreview.hangs(Blocks.LADDER.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.WALL_TORCH.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.SOUL_WALL_TORCH.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.COPPER_WALL_TORCH.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.REDSTONE_WALL_TORCH.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.OAK_WALL_SIGN.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.WALL_BANNER.pick(DyeColor.RED).defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.TRIPWIRE_HOOK.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.TUBE_CORAL_WALL_FAN.defaultBlockState()));
		assertTrue(DisablePreview.hangs(Blocks.COCOA.defaultBlockState()));
		assertTrue(DisablePreview.hangs(on(Blocks.STONE_BUTTON.defaultBlockState(), AttachFace.WALL)));
		assertTrue(DisablePreview.hangs(on(Blocks.LEVER.defaultBlockState(), AttachFace.WALL)));
	}

	@Test
	void blocksThatDontHangOnTheBlockBehindThem() {
		assertFalse(DisablePreview.hangs(Blocks.TORCH.defaultBlockState()));
		assertFalse(DisablePreview.hangs(Blocks.OAK_SIGN.defaultBlockState()));
		assertFalse(DisablePreview.hangs(on(Blocks.STONE_BUTTON.defaultBlockState(), AttachFace.FLOOR)));
		assertFalse(DisablePreview.hangs(on(Blocks.LEVER.defaultBlockState(), AttachFace.CEILING)));
		assertFalse(DisablePreview.hangs(on(Blocks.GRINDSTONE.defaultBlockState(), AttachFace.WALL)));
		assertFalse(DisablePreview.hangs(Blocks.SKELETON_WALL_SKULL.defaultBlockState()));
		assertFalse(DisablePreview.hangs(Blocks.OAK_WALL_HANGING_SIGN.defaultBlockState()));
		assertFalse(DisablePreview.hangs(Blocks.OAK_STAIRS.defaultBlockState()
			.setValue(StairBlock.FACING, Direction.EAST)));
		assertFalse(DisablePreview.hangs(Blocks.CHEST.defaultBlockState()));
	}

	private static BlockState on(BlockState state, AttachFace face) {
		return state.setValue(FaceAttachedHorizontalDirectionalBlock.FACE, face);
	}
}
