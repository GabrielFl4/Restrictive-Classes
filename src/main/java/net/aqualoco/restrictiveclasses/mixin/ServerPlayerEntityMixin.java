package net.aqualoco.restrictiveclasses.mixin;

import net.aqualoco.restrictiveclasses.role.RoleRegistry;
import net.aqualoco.restrictiveclasses.state.RoleHolder;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements RoleHolder {
    @Unique private String rc$roleId = RoleRegistry.DEFAULT_ROLE;

    @Override public String rc$getRoleId() { return rc$roleId; }
    @Override public void rc$setRoleId(String id) { rc$roleId = id; }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void rc$copyRoleOnRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.rc$setRoleId(((RoleHolder) oldPlayer).rc$getRoleId());
    }
}
