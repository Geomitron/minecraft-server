package dev.buildingplanner.server;

import static dev.buildingplanner.server.LayerNetwork.NETHER;
import static dev.buildingplanner.server.LayerNetwork.OVERWORLD;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.buildingplanner.server.LayerNetwork.Game;
import dev.buildingplanner.sync.LayerChange;
import dev.buildingplanner.sync.Payloads;
import dev.buildingplanner.sync.PlayerMessage;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Each player's undo and redo on the server's layer, in any dimension. */
class LayerUndoTest {
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(2, 64, 1);
	private static final BlockPos C = new BlockPos(3, 64, 1);
	private static BlockState stone;
	private static BlockState dirt;
	private static BlockState glass;
	private static BlockState air;

	private LayerNetwork network;
	private Game alex;
	private Game blair;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		stone = Blocks.STONE.defaultBlockState();
		dirt = Blocks.DIRT.defaultBlockState();
		glass = Blocks.GLASS.defaultBlockState();
		air = Blocks.AIR.defaultBlockState();
	}

	@BeforeEach
	void start() {
		network = new LayerNetwork();
		alex = network.join("Alex");
		blair = network.join("Blair");
		alex.watchAround(1);
		blair.watchAround(1);
		network.settle();
	}

	private void edit(Game game, BlockState state, BlockPos... positions) {
		game.edit(List.of(positions).stream().map(pos -> new LayerChange(pos, state)).toArray(LayerChange[]::new));
		network.settle();
	}

	private void undo(Game game) {
		game.undo();
		network.settle();
	}

	private void redo(Game game) {
		game.redo();
		network.settle();
	}

	private BlockState at(BlockPos pos) {
		return network.server.get(OVERWORLD, pos);
	}

	@Test
	void undoTakesBackAWholeEditAndEveryWatcherSeesIt() {
		edit(alex, stone, A, B);

		undo(alex);

		assertEquals(air, at(A));
		assertEquals(air, at(B));
		Payloads.Applied undone = blair.applied().getLast();
		assertEquals(Payloads.NO_CLIENT_SEQ, undone.clientSeq());
		assertEquals(alex.id, undone.author());
		network.assertSameAsServer(alex);
		network.assertSameAsServer(blair);
	}

	@Test
	void undoTakesBackOnlyThePlayersOwnEditsNewestFirst() {
		edit(alex, stone, A);
		edit(blair, dirt, B);
		edit(alex, glass, C);

		undo(alex);
		assertEquals(air, at(C));
		assertEquals(dirt, at(B));
		assertEquals(stone, at(A));

		undo(alex);
		assertEquals(air, at(A));
		assertEquals(dirt, at(B));
	}

	@Test
	void blocksSomeoneElseChangedSinceStayAndANoticeSaysHowMany() {
		edit(alex, stone, A, B, C);
		edit(blair, dirt, A, B);

		undo(alex);

		assertEquals(dirt, at(A));
		assertEquals(dirt, at(B));
		assertEquals(air, at(C));
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(2)), alex.notices);

		// Redo takes forward only the part the undo changed.
		redo(alex);
		assertEquals(stone, at(C));
		assertEquals(dirt, at(A));
	}

	@Test
	void anUndoThatChangesNoBlockSendsNoApplied() {
		edit(alex, stone, A);
		edit(blair, dirt, A);
		int appliedBefore = blair.applied().size();

		undo(alex);

		assertEquals(appliedBefore, blair.applied().size());
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(1)), alex.notices);
		// The step is spent: nothing is left to undo or redo.
		undo(alex);
		redo(alex);
		assertEquals(List.of(PlayerMessage.blocksLeftAsTheyAre(1), PlayerMessage.nothingToUndo(),
			PlayerMessage.nothingToRedo()), alex.notices);
	}

	@Test
	void undoAndRedoReachEditsInADimensionThePlayerLeft() {
		edit(alex, stone, A);
		alex.moveTo(NETHER);
		alex.watchAround(1);
		network.settle();
		edit(alex, glass, A);

		undo(alex);
		assertEquals(air, network.server.get(NETHER, A));
		assertEquals(stone, at(A));

		// The overworld undo reaches Blair, who watches there, and not Alex's game in the Nether.
		int alexApplied = alex.applied().size();
		undo(alex);
		assertEquals(air, at(A));
		assertEquals(OVERWORLD, blair.applied().getLast().dimension());
		assertEquals(alexApplied, alex.applied().size());
		network.assertSameAsServer(blair);

		redo(alex);
		assertEquals(stone, at(A));
		redo(alex);
		assertEquals(glass, network.server.get(NETHER, A));
		network.assertSameAsServer(alex);
		network.assertSameAsServer(blair);
	}

	@Test
	void aNewEditClearsTheRedoList() {
		edit(alex, stone, A);
		undo(alex);

		edit(alex, dirt, B);
		redo(alex);

		assertEquals(air, at(A));
		assertEquals(List.of(PlayerMessage.nothingToRedo()), alex.notices);
	}

	@Test
	void anEditThatChangesNoBlockKeepsTheRedoList() {
		edit(alex, stone, A);
		undo(alex);

		edit(alex, air, B);
		redo(alex);

		assertEquals(stone, at(A));
	}

	@Test
	void stepsSurviveLeavingAndKeepTheNewestFifty() {
		for (int i = 0; i < UndoHistory.DEPTH + 1; i++) {
			edit(alex, stone, new BlockPos(i % 16, 64 + i / 16, 0));
		}
		alex.disconnect();
		alex.reconnect();

		for (int i = 0; i < UndoHistory.DEPTH + 1; i++) {
			alex.undo();
		}
		network.settle();

		assertEquals(stone, at(new BlockPos(0, 64, 0)), "the oldest edit is past the depth");
		assertEquals(air, at(new BlockPos(1, 64, 0)));
		assertEquals(air, at(new BlockPos(UndoHistory.DEPTH % 16, 64 + UndoHistory.DEPTH / 16, 0)));
		assertEquals(List.of(PlayerMessage.nothingToUndo()), alex.notices);
	}
}
