package net.aqualoco.restrictiveclasses.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import net.aqualoco.restrictiveclasses.role.Role;
import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.aqualoco.restrictiveclasses.network.payload.RulesSyncS2CPayload;
import net.aqualoco.restrictiveclasses.network.payload.RoleSyncS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public final class RCNetwork {
    private static final Gson GSON = new GsonBuilder().create();
    private RCNetwork() {}

    public static void sendRulesTo(ServerPlayerEntity player) {
        long version = computeRulesVersion();

        JsonArray arr = new JsonArray();
        for (Role r : RoleRegistry.all()) {
            arr.add(GSON.toJsonTree(r)); // serializa cada role como JSON
        }
        String jsonArray = GSON.toJson(arr);

        ServerPlayNetworking.send(player, new RulesSyncS2CPayload(version, jsonArray));
    }

    public static void sendRoleTo(ServerPlayerEntity player) {
        String roleId = ((RoleHolder) player).rc$getRoleId();
        ServerPlayNetworking.send(player, new RoleSyncS2CPayload(roleId));
    }

    public static void broadcastRules(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            sendRulesTo(p);
            sendRoleTo(p);
        }
    }

    private static long computeRulesVersion() {
        CRC32 crc = new CRC32();
        for (Role r : RoleRegistry.all()) {
            crc.update(r.id().getBytes(StandardCharsets.UTF_8));
        }
        return crc.getValue();
    }
}
