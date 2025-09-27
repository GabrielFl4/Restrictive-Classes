package net.aqualoco.restrictiveclasses.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RoleStorage extends PersistentState {
    private static final String SAVE_NAME = "restrictive_classes_roles";

    /** mapa persistido: UUID -> roleId */
    private final Map<UUID, String> roles = new HashMap<>();

    /** Codec do mapa de roles */
    private static final Codec<Map<UUID, String>> ROLES_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Codec.STRING
            );

    /** Codec da RoleStorage (root tem o campo "roles") */
    private static final Codec<RoleStorage> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    ROLES_CODEC.fieldOf("roles").forGetter(s -> s.roles)
            ).apply(inst, map -> {
                RoleStorage s = new RoleStorage();
                s.roles.putAll(map);
                return s;
            }));

    /** Tipo usado pelo PersistentStateManager (com CODEC) */
    public static final PersistentStateType<RoleStorage> TYPE =
            new PersistentStateType<>(SAVE_NAME, RoleStorage::new, CODEC, DataFixTypes.LEVEL);

    public RoleStorage() {}

    /** Pega (ou cria) o storage do mundo */
    public static RoleStorage get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager psm = overworld.getPersistentStateManager();
        // 1.21.6+: aceita só o TYPE
        return psm.getOrCreate(TYPE);
    }

    // ---------- API ----------
    public Optional<String> getRole(UUID uuid) {
        String s = roles.get(uuid);
        // sanitiza saves antigos do "Optional[xxx]"
        if (s != null && s.startsWith("Optional[") && s.endsWith("]")) {
            s = s.substring("Optional[".length(), s.length() - 1);
            roles.put(uuid, s);
            markDirty();
        }
        return Optional.ofNullable(s);
    }

    public void setRole(UUID uuid, String roleId) {
        if (roleId == null) roles.remove(uuid);
        else roles.put(uuid, roleId);
        markDirty();
    }
}
