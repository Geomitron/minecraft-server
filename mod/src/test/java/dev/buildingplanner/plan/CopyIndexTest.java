package dev.buildingplanner.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CopyIndexTest {
	@TempDir
	Path folder;

	@BeforeAll
	static void bootstrap() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	private static CopyHistory history() {
		CopyHistory history = CopyHistory.EMPTY;
		history = history.add("Nov 19, 6:21 PM", 1_763_576_460_000L, new Vec3i(12, 5, 8), 120).history();
		history = history.add("Nov 19, 6:30 PM", 1_763_577_000_000L, new Vec3i(1, 1, 1), 1).history();
		return history.rename(1, "Hut");
	}

	@Test
	void theIndexIsWrittenAndReadBack() throws IOException {
		CopyIndex.write(folder, history());

		assertEquals(history(), CopyIndex.read(folder));
		CompoundTag root = NbtIo.readCompressed(folder.resolve("index.dat"), NbtAccounter.unlimitedHeap());
		assertEquals(1, root.getIntOr("version", 0));
		assertEquals(3, root.getIntOr("next", 0));
		CompoundTag hut = root.getListOrEmpty("copies").getCompoundOrEmpty(1);
		assertEquals(1, hut.getIntOr("file", 0));
		assertEquals("Hut", hut.getStringOr("name", ""));
		assertEquals(1_763_576_460_000L, hut.getLongOr("time", 0));
		assertEquals(1, hut.getByteOr("kept", (byte) 0));
		assertEquals(12, hut.getCompoundOrEmpty("size").getIntOr("x", 0));
		assertEquals(120, hut.getIntOr("blocks", 0));
		assertFalse(Files.exists(folder.resolve("index.dat.tmp")), "written through a temporary file and a rename");
	}

	@Test
	void noIndexIsNoCopies() {
		assertEquals(CopyHistory.EMPTY, CopyIndex.read(folder.resolve("missing")));
	}

	@Test
	void anUnreadableIndexStartsAnEmptyListAndIsRenamed() throws IOException {
		Path index = folder.resolve("index.dat");
		Files.writeString(index, "not an index", StandardCharsets.UTF_8);
		Files.writeString(folder.resolve("1.litematic"), "a copy's file", StandardCharsets.UTF_8);

		assertEquals(CopyHistory.EMPTY, CopyIndex.read(folder));
		assertFalse(Files.exists(index));
		assertEquals("not an index", Files.readString(folder.resolve("index.dat.unreadable"), StandardCharsets.UTF_8));
		assertTrue(Files.exists(folder.resolve("1.litematic")), "the copies' files stay");
	}

	@Test
	void anIndexThatBreaksItsRulesIsUnreadable() {
		assertUnreadable(root -> root.putInt("version", 2));
		assertUnreadable(root -> root.remove("next"));
		assertUnreadable(root -> root.putInt("next", 2));
		assertUnreadable(root -> copy(root, 0).putInt("file", 1));
		assertUnreadable(root -> copy(root, 0).putString("name", ""));
		assertUnreadable(root -> copy(root, 0).getCompoundOrEmpty("size").putInt("y", 0));
		assertUnreadable(root -> copy(root, 0).putInt("blocks", -1));
		assertUnreadable(root -> copy(root, 0).remove("kept"));
	}

	private static CompoundTag copy(CompoundTag root, int index) {
		ListTag copies = root.getListOrEmpty("copies");
		return copies.getCompoundOrEmpty(index);
	}

	private static void assertUnreadable(Consumer<CompoundTag> change) {
		CompoundTag root = CopyIndex.toTag(history());
		change.accept(root);
		assertThrows(IllegalArgumentException.class, () -> CopyIndex.fromTag(root));
	}
}
