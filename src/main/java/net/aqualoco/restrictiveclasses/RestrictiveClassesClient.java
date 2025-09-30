package net.aqualoco.restrictiveclasses;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.aqualoco.restrictiveclasses.client.ClientGates;
import net.aqualoco.restrictiveclasses.client.ClientRules;
import net.aqualoco.restrictiveclasses.client.ui.TooltipHandler;
import net.aqualoco.restrictiveclasses.network.payload.RoleSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.RulesSyncS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class RestrictiveClassesClient implements ClientModInitializer {
    private static final Gson GSON = new Gson();

    @Override
    public void onInitializeClient() {
        // Tick que abre a tela quando snapshot+role estiverem prontos
        ClientGates.register();

        // Assim que conectar num mundo, marcamos para abrir (o tick só abrirá quando isReady && unassigned)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientGates.markPendingOpen());

        // Snapshot de regras (S2C)
        ClientPlayNetworking.registerGlobalReceiver(RulesSyncS2CPayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                JsonArray arr = JsonParser.parseString(payload.rolesJsonArray()).getAsJsonArray();
                List<String> list = new ArrayList<>(arr.size());
                arr.forEach(el -> list.add(GSON.toJson(el)));

                ClientRules.get().applyRulesSnapshot(payload.version(), list);
                net.aqualoco.restrictiveclasses.core.RC.LOG.info("[RC/Client] rules_sync recebido: roles={}", list.size());

                if (ClientRules.get().isUnassigned()) ClientGates.markPendingOpen();
            });
        });

        // Role atual (S2C)
        ClientPlayNetworking.registerGlobalReceiver(RoleSyncS2CPayload.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                ClientRules.get().applyRole(payload.roleId());
                net.aqualoco.restrictiveclasses.core.RC.LOG.info("[RC/Client] role atual: {}", payload.roleId());

                if (ClientRules.get().isUnassigned() && ClientRules.get().isReady()) {
                    ClientGates.markPendingOpen();
                }
            });
        });

        // Tooltips
        TooltipHandler.register();
    }
}
