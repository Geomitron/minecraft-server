package dev.buildingplanner.client;

import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.modifier.ArrayModifier;
import nl.requios.effortlessbuilding.modifier.ModifierSystem;

/**
 * The driver's commands for Effortless Building, in a class of their own so the driver still loads on a client without
 * that mod.
 */
final class EffortlessDriver {
	private EffortlessDriver() {
	}

	/** Effortless Building's build mode, as its radial menu sets it. */
	static void mode(String mode) {
		BuildModes.CLIENT.setBuildMode(BuildModeEnum.valueOf(mode));
	}

	/** Effortless Building's array modifier alone; a count of 0 clears it. */
	static void array(int count, int dx, int dy, int dz) {
		ModifierSystem.CLIENT.clearModifiers();
		if (count > 0) {
			ArrayModifier array = new ArrayModifier();
			array.count = count;
			array.offsetX = dx;
			array.offsetY = dy;
			array.offsetZ = dz;
			ModifierSystem.CLIENT.addModifier(array);
		}
	}
}
