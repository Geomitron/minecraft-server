package dev.buildingplanner.client;

import dev.buildingplanner.layer.DeleteMarks;
import dev.buildingplanner.layer.Layer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Delete by aim's marks as the driver reads them, and the times its aims and its marks' shape take (step9.sh). */
final class MarksProbe {
	/** The most marks {@link #marks} lists one by one. */
	private static final int LISTED = 40;

	private MarksProbe() {
	}

	/**
	 * The session's marks: how many, how many positions erasing them would take, then each mark as x,y,z, lowest x first,
	 * then y, then z, when there are 1 to 40.
	 */
	static String marks(ClientSession session) {
		DeleteMarks marks = session.marks();
		List<BlockPos> marked = new ArrayList<>();
		marks.positions().forEach(key -> marked.add(BlockPos.of(key)));
		marked.sort(Comparator.comparingInt((BlockPos p) -> p.getX()).thenComparingInt(p -> p.getY())
			.thenComparingInt(p -> p.getZ()));
		String counts = marks.size() + " marks, " + marks.eraseSize() + " to erase";
		if (marked.isEmpty() || marked.size() > LISTED) {
			return counts;
		}
		return counts + ": " + marked.stream().map(p -> p.getX() + "," + p.getY() + "," + p.getZ())
			.collect(Collectors.joining(" "));
	}

	/** Marks every ghost block in the box, as one sweep over each would, and gives how many marks there are. */
	static String markAll(ClientSession session, BlockPos from, BlockPos to) {
		DeleteMarks marks = session.marks();
		Layer layer = session.replica().layer();
		marks.startSweep();
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			marks.pass(pos, layer);
		}
		marks.endSweep();
		return String.valueOf(marks.size());
	}

	/**
	 * Times a frame's sweep from looking at {@code from} to looking at {@code to}, from the player's eyes, aiming at the
	 * game's level and the session's layer as {@link DeleteAim} does, {@code repeats} times: how many aims each takes,
	 * and their mean and longest time in milliseconds.
	 */
	static String timeAims(ClientSession session, Vec3 from, Vec3 to, int repeats) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 eye = mc.player.getEyePosition();
		DeleteMarks.View start = new DeleteMarks.View(eye, from.subtract(eye).normalize());
		DeleteMarks.View end = new DeleteMarks.View(eye, to.subtract(eye).normalize());
		DeleteMarks.Tracer tracer = DeleteAim.tracer(mc.level, session.replica().layer());
		int[] aims = {0};
		DeleteMarks.Tracer counting = (e, look) -> {
			aims[0]++;
			return tracer.trace(e, look);
		};
		long total = 0;
		long longest = 0;
		for (int i = 0; i < repeats; i++) {
			aims[0] = 0;
			long began = System.nanoTime();
			DeleteMarks.swept(start, end, counting);
			long took = System.nanoTime() - began;
			total += took;
			longest = Math.max(longest, took);
		}
		return String.format(Locale.ROOT, "%d aims, mean %.2f ms, longest %.2f ms", aims[0], total / 1e6 / repeats,
			longest / 1e6);
	}

	/**
	 * Times building the shape of the session's marks, on a copy of them, so the session's own outline still sees each
	 * change: see {@link #timeOutline(DeleteMarks, Layer)}.
	 */
	static String timeOutline(ClientSession session) {
		Layer layer = session.replica().layer();
		DeleteMarks copy = new DeleteMarks(Integer.MAX_VALUE);
		copy.startSweep();
		session.marks().positions().forEach(key -> copy.pass(BlockPos.of(key), layer));
		copy.endSweep();
		return timeOutline(copy, layer);
	}

	/**
	 * Times building the shape of {@code count} marks that touch no other mark, on every other position of a cube of
	 * ghost stone bricks, so each draws all its faces and edges: the most a shape of that many marks can hold.
	 */
	static String timeScatteredOutline(int count) {
		Layer layer = new Layer();
		BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
		DeleteMarks marks = new DeleteMarks(count);
		int side = (int) Math.ceil(Math.cbrt(2.0 * count));
		marks.startSweep();
		for (int x = 0; x < side && marks.size() < count; x++) {
			for (int y = 0; y < side && marks.size() < count; y++) {
				for (int z = (x + y) % 2; z < side && marks.size() < count; z += 2) {
					BlockPos pos = new BlockPos(x, -60 + y, z);
					layer.set(pos, bricks);
					marks.pass(pos, layer);
				}
			}
		}
		marks.endSweep();
		return timeOutline(marks, layer);
	}

	/**
	 * Times building the whole shape of {@code marks}, as when Aim shows them again, then building it again after one
	 * mark changes, as in a frame of a sweep: 20 marks spread over them, each unmarked and marked again.
	 */
	private static String timeOutline(DeleteMarks marks, Layer layer) {
		MarksOutline outline = new MarksOutline(() -> null);
		long began = System.nanoTime();
		outline.update(marks);
		long whole = System.nanoTime() - began;
		long[] keys = marks.positions().toLongArray();
		long total = 0;
		long longest = 0;
		int changes = 0;
		for (int i = 0; i < 20; i++) {
			BlockPos pos = BlockPos.of(keys[(int) ((long) i * keys.length / 20)]);
			for (int pass = 0; pass < 2; pass++) {
				marks.startSweep();
				marks.pass(pos, layer);
				marks.endSweep();
				long start = System.nanoTime();
				outline.update(marks);
				long took = System.nanoTime() - start;
				total += took;
				longest = Math.max(longest, took);
				changes++;
			}
		}
		outline.close();
		return String.format(Locale.ROOT, "%d marks built in %.1f ms; after one changed mark, %.2f ms mean, %.2f ms"
			+ " longest", marks.size(), whole / 1e6, total / 1e6 / changes, longest / 1e6);
	}
}
