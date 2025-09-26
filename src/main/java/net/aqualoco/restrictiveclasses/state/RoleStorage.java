package net.aqualoco.restrictiveclasses.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public class RoleStorage extends PersistentState implements RoleStorageInterface {
    private static final String SAVE_NAME = "restrictive_classes_roles";

    private final Map<UUID, String> roles = new HashMap<>();

    // Codec para (UUID -> roleId)
    private static final Codec<Map<UUID, String>> ROLES_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Codec.STRING
            );

    // Codec do próprio RoleStorage (mapeia só o campo roles)
    private static final Codec<RoleStorage> CODEC =
            ROLES_CODEC.xmap(
                    map -> {
                        RoleStorage s = new RoleStorage();
                        s.roles.putAll(map);
                        return s;
                    },
                    s -> s.roles
            );

    // Tipo registrado para o PersistentStateManager (id, construtor, codec, datafix)
    private static final PersistentStateType<RoleStorage> TYPE =
            new PersistentStateType<>(SAVE_NAME, RoleStorage::new, CODEC, DataFixTypes.LEVEL);

    public RoleStorage() {}

    public static RoleStorage get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager psm = overworld.getPersistentStateManager();
        return psm.getOrCreate(TYPE); // apenas 1 argumento na 1.21.6+
    }

    // Assinatura nova em 1.21.x
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DataResult<NbtElement> enc = ROLES_CODEC.encodeStart(NbtOps.INSTANCE, roles);
        enc.result().ifPresent(elem -> nbt.put("roles", elem));
        return nbt;
    }

    // Helpers
    public Optional<String> getRole(UUID uuid) { return Optional.ofNullable(roles.get(uuid)); }
    public void setRole(UUID uuid, String roleId) { if (roleId == null) roles.remove(uuid); else roles.put(uuid, roleId); markDirty(); }
    public void clearRole(UUID uuid) { roles.remove(uuid); markDirty(); }
    public Map<UUID, String> debugAll() { return Collections.unmodifiableMap(roles); }
}
