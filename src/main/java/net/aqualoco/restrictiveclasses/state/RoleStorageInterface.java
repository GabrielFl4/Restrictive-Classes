package net.aqualoco.restrictiveclasses.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

public interface RoleStorageInterface {
    // Assinatura nova em 1.21.x
    NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries);
}
