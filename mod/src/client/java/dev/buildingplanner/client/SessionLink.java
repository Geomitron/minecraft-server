package dev.buildingplanner.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * What a session sends its payloads through: the network, to a server that runs Building Planner, or the player's
 * own {@link LocalSession} on a server that doesn't.
 */
interface SessionLink {
	/** The connection to a server that runs Building Planner. */
	SessionLink NETWORK = new SessionLink() {
		@Override
		public void send(CustomPacketPayload payload) {
			ClientPlayNetworking.send(payload);
		}

		@Override
		public boolean local() {
			return false;
		}
	};

	void send(CustomPacketPayload payload);

	/** Whether the session runs on the player's computer, with a server that doesn't run Building Planner. */
	boolean local();
}
