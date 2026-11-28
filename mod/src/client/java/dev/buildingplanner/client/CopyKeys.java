package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.input.CopyNudge;
import dev.buildingplanner.input.KeyRepeat;
import dev.buildingplanner.look.ControlsLine;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * The copy's eight keys (see "Keys"): "Lock or unlock" (H), the six "Nudge" keys (the arrow keys, Page Up and Page
 * Down) and "Change copy mode" (R), Satisfactory's keys for its hologram. They need no hook: the client tick reads
 * them, with no screen open, while the copy is at the aim, with a set fixed too, and otherwise empties their presses, so
 * a press made before the copy came to the aim doesn't act once it does. A nudge key acts only while the copy is locked,
 * and held down it acts again ({@link KeyRepeat}); during a set it moves the first copy, and the set with it. H and the
 * nudge keys act the same way for the build modes' lock while its controls are active, in every mode but Disable (see
 * "Locking and the first click"), where R does nothing. R does nothing in Delete.
 */
final class CopyKeys {
	static final KeyMapping LOCK =
		new KeyMapping("key.buildingplanner.lock_copy", InputConstants.KEY_H, BuildingPlannerClient.category());
	static final Map<CopyNudge, KeyMapping> NUDGES = new EnumMap<>(CopyNudge.class);
	static final KeyMapping MODE =
		new KeyMapping("key.buildingplanner.copy_mode", InputConstants.KEY_R, BuildingPlannerClient.category());

	static {
		Map<CopyNudge, Integer> defaults = Map.of(CopyNudge.FORWARD, InputConstants.KEY_UP, CopyNudge.BACK,
			InputConstants.KEY_DOWN, CopyNudge.LEFT, InputConstants.KEY_LEFT, CopyNudge.RIGHT, InputConstants.KEY_RIGHT,
			CopyNudge.UP, InputConstants.KEY_PAGEUP, CopyNudge.DOWN, InputConstants.KEY_PAGEDOWN);
		for (CopyNudge nudge : CopyNudge.values()) {
			NUDGES.put(nudge, new KeyMapping(nudge.translationKey(), defaults.get(nudge), BuildingPlannerClient.category()));
		}
	}

	private final Map<CopyNudge, KeyRepeat> repeats = new EnumMap<>(CopyNudge.class);

	CopyKeys() {
		for (CopyNudge nudge : CopyNudge.values()) {
			repeats.put(nudge, new KeyRepeat());
		}
	}

	/** Registers the eight keys; call while the client starts, before the game reads its options. */
	static void register() {
		KeyMappingHelper.registerKeyMapping(LOCK);
		NUDGES.values().forEach(KeyMappingHelper::registerKeyMapping);
		KeyMappingHelper.registerKeyMapping(MODE);
	}

	/**
	 * The copy's keys by their names as the game gives them (as Controls shows them), for its controls line, with
	 * Minecraft's sprint key, which lines the copy up, and the key for "Saved copies"; a key with no key bound has no
	 * name.
	 */
	static ControlsLine.Keys names(Minecraft mc) {
		List<CopyNudge> along = List.of(CopyNudge.FORWARD, CopyNudge.BACK, CopyNudge.LEFT, CopyNudge.RIGHT);
		List<String> arrows = List.of("key.keyboard.up", "key.keyboard.down", "key.keyboard.left", "key.keyboard.right");
		boolean onArrows = true;
		List<String> nudges = new ArrayList<>(along.size());
		for (int i = 0; i < along.size(); i++) {
			KeyMapping key = NUDGES.get(along.get(i));
			onArrows &= key.saveString().equals(arrows.get(i));
			nudges.add(name(key));
		}
		return new ControlsLine.Keys(name(LOCK), name(MODE), nudges, onArrows, name(NUDGES.get(CopyNudge.UP)),
			name(NUDGES.get(CopyNudge.DOWN)), name(mc.options.keySprint), name(BuildingPlannerClient.SAVED_COPIES));
	}

	private static String name(KeyMapping key) {
		return key.isUnbound() ? null : key.getTranslatedKeyMessage().getString();
	}

	/**
	 * Once a client tick, after the session's: acts on the keys for the copy at the aim, the build modes' lock or
	 * Delete, or empties their presses.
	 */
	void tick(Minecraft mc, ClientSession session) {
		boolean acting = session != null && session.clipboard().atAim() && mc.gui.screen() == null;
		boolean locking = !acting && session != null && mc.gui.screen() == null && BuildingPlannerClient.lockActive();
		int lockPresses = presses(LOCK);
		int modePresses = presses(MODE);
		if (acting) {
			for (int i = 0; i < lockPresses; i++) {
				session.toggleCopyLock();
			}
			for (int i = 0; i < modePresses; i++) {
				session.cycleCopyMode();
			}
		} else if (locking) {
			for (int i = 0; i < lockPresses; i++) {
				BuildingPlannerClient.toggleLock();
			}
		}
		Entity view = Aim.entity();
		boolean nudging = view != null
			&& (acting && session.clipboard().locked() || locking && session.lock().on(EffortlessShapes.step()));
		for (CopyNudge nudge : CopyNudge.values()) {
			KeyMapping key = NUDGES.get(nudge);
			int presses = presses(key);
			KeyRepeat repeat = repeats.get(nudge);
			if (!nudging) {
				repeat.reset();
				continue;
			}
			for (int i = repeat.tick(presses, key.isDown()); i > 0; i--) {
				if (acting) {
					session.nudgeCopy(nudge.direction(view.getYRot()));
				} else {
					BuildingPlannerClient.nudgeLock(nudge.direction(view.getYRot()));
				}
			}
		}
	}

	private static int presses(KeyMapping key) {
		int presses = 0;
		while (key.consumeClick()) {
			presses++;
		}
		return presses;
	}
}
