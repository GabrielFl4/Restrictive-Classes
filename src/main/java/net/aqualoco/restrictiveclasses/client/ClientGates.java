package net.aqualoco.restrictiveclasses.client;

import net.aqualoco.restrictiveclasses.client.ui.RoleSelectScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class ClientGates {
    private static boolean pendingOpen = false;

    private ClientGates() {}

    public static void markPendingOpen() { pendingOpen = true; }

    public static void register() {
        // ClientGates.register() é chamado no client initializer
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!pendingOpen) return;
            if (client.currentScreen != null) return;
            if (!ClientRules.get().isReady()) return;
            if (!ClientRules.get().isUnassigned()) { pendingOpen = false; return; }

            client.setScreen(new RoleSelectScreen());
            pendingOpen = false;
        });
    }
}
