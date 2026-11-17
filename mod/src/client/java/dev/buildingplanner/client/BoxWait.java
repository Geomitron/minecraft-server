package dev.buildingplanner.client;

import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildpipeline.BuildPipelineClient;

/**
 * Copy, "Copy real blocks" and the material list waiting for a box drawn with Effortless Building's "Cube" shape: the
 * wait switches Effortless Building to "Cube", remembering the player's mode, and puts that mode back when the box is
 * drawn or the wait is stopped. Choosing another mode ends the wait and keeps that mode. Only made with Effortless
 * Building loaded, whose classes it uses; its methods take and return none of them.
 */
final class BoxWait {
	/** What the box is for. */
	enum Purpose {
		/** The ghost blocks in the box become the copy. */
		COPY,
		/** The real blocks in the box become the copy. */
		COPY_REAL,
		/** Litematica's material list opens for the ghost blocks in the box. */
		MATERIAL_LIST
	}

	private Purpose purpose;
	private BuildModeEnum before;
	/**
	 * Whether the player's mode goes back at the next tick, after Effortless Building has ended the cube that drew the
	 * box: switching to Disable within the click would sound as a cancelled shape.
	 */
	private boolean restoreAtTick;

	/** What the awaited box is for, or null while none is awaited. */
	Purpose purpose() {
		return purpose;
	}

	/** Starts waiting for a box, switching to "Cube" and dropping any shape under way. */
	void start(Purpose purpose) {
		before = restoreAtTick ? before : BuildModes.CLIENT.getBuildMode();
		restoreAtTick = false;
		BuildPipelineClient.cancelCurrentSequence();
		BuildModes.CLIENT.setBuildMode(BuildModeEnum.CUBE);
		this.purpose = purpose;
		ClickRule.setBoxAwaited(true);
	}

	/**
	 * Stops waiting, dropping a box under way and putting back the player's mode, or puts it back now after a box was
	 * drawn; otherwise does nothing.
	 */
	void stop() {
		if (purpose == null) {
			if (restoreAtTick) {
				tick();
			}
			return;
		}
		end();
		BuildPipelineClient.cancelCurrentSequence();
		BuildModes.CLIENT.setBuildMode(before);
	}

	/**
	 * The box was drawn, within Effortless Building's click: returns what it's for; the player's mode goes back at the
	 * next tick.
	 */
	Purpose take() {
		Purpose drawn = purpose;
		end();
		restoreAtTick = true;
		return drawn;
	}

	/**
	 * Once a client tick: puts back the player's mode after a box was drawn, and ends the wait when the player chose
	 * another mode, which stays.
	 */
	void tick() {
		if (restoreAtTick) {
			restoreAtTick = false;
			if (BuildModes.CLIENT.getBuildMode() == BuildModeEnum.CUBE) {
				BuildModes.CLIENT.setBuildMode(before);
			}
		}
		if (purpose != null && BuildModes.CLIENT.getBuildMode() != BuildModeEnum.CUBE) {
			end();
		}
	}

	private void end() {
		purpose = null;
		ClickRule.setBoxAwaited(false);
	}
}
