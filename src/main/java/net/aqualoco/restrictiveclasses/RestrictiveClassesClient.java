package net.aqualoco.restrictiveclasses;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.aqualoco.restrictiveclasses.client.ClientRules;
import net.aqualoco.restrictiveclasses.network.payload.RoleSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.RulesSyncS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;


import java.util.ArrayList;
import java.util.List;
import static net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil.GSON;

public class RestrictiveClassesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RulesSyncS2CPayload.ID,
                (payload, context) -> {
                    long version = payload.version();
                    String arrayJson = payload.rolesJsonArray();

                    JsonArray arr = JsonParser.parseString(arrayJson).getAsJsonArray();
                    List<String> roleJsons = new ArrayList<>(arr.size());
                    arr.forEach(el -> roleJsons.add(GSON.toJson(el)));

                    context.client().execute(() ->
                            ClientRules.get().applyRulesSnapshot(version, roleJsons)
                    );
                });

        ClientPlayNetworking.registerGlobalReceiver(RoleSyncS2CPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        ClientRules.get().applyRole(payload.roleId())
                ));
    }
}
