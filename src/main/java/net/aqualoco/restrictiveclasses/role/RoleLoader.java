package net.aqualoco.restrictiveclasses.role;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.aqualoco.restrictiveclasses.core.RC;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class RoleLoader implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return RC.id("roles_loader");
    }

    @Override
    public void reload(ResourceManager manager) {
        RoleRegistry.clear();
        // Lê todos os JSONs em data/<ns>/roles/*.json
        var resources = manager.findResources("roles", p -> p.getPath().endsWith(".json"));
        int count = 0;
        for (var entry : resources.entrySet()) {
            try (var reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8))) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                // Pode ser um único objeto Role ou um array [{...}, {...}]
                if (json.isJsonArray()) {
                    for (var el : json.getAsJsonArray()) {
                        Role role = GSON.fromJson(el, Role.class);
                        RoleRegistry.put(role);
                        count++;
                    }
                } else {
                    Role role = GSON.fromJson(json, Role.class);
                    RoleRegistry.put(role);
                    count++;
                }
            } catch (Exception e) {
                RC.LOG.error("[RC] Falha ao carregar role em {}: {}", entry.getKey(), e.toString());
            }
        }
        RC.LOG.info("[RC] Roles carregadas: {}", count);
    }
}
