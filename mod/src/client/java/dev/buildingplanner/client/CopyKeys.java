package dev.buildingplanner.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.buildingplanner.input.CopyNudge;
import dev.buildingplanner.input.KeyRepeat;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * The copy's own keys (see "Keys"): "Lock or unlock the copy" (H), the six "Nudge the copy" keys (the arrow keys, Page
 * Up and Page Down) and "Change copy mode" (R), Satisfactory's keys for its hologram. They need no hook: the client
 * tick reads them, with no screen open, while the copy is at the aim and no zoop's row is fixed, and otherwise empties
 * their presses, so a press made before the copy came to the aim doesn't act once it does. A nudge key acts only while
 * the copy is locked, and held down it acts again ({@link KeyRepeat}).
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

	/** Once a client tick, after the session's: acts on the keys for the copy at the aim, or empties their presses. */
	void tick(Minecraft mc, ClientSession session) {
		boolean acting = session != null && session.clipboard().atAim() && !session.clipboard().rowFixed()
			&& mc.gui.screen() == null;
		int lockPresses = presses(LOCK);
		int modePresses = presses(MODE);
		if (acting) {
			for (int i = 0; i < lockPresses; i++) {
				session.toggleCopyLock();
			}
			for (int i = 0; i < modePresses; i++) {
				session.cycleCopyMode();
			}
		}
		Entity view = Aim.entity();
		boolean nudging = acting && session.clipboard().locked() && view != null;
		for (CopyNudge nudge : CopyNudge.values()) {
			KeyMapping key = NUDGES.get(nudge);
			int presses = presses(key);
			KeyRepeat repeat = repeats.get(nudge);
			if (!nudging) {
				repeat.reset();
				continue;
			}
			for (int i = repeat.tick(presses, key.isDown()); i > 0; i--) {
				session.nudgeCopy(nudge.direction(view.getYRot()));
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
