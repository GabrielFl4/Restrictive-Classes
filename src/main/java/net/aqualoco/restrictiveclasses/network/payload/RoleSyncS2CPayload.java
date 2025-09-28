package net.aqualoco.restrictiveclasses.network.payload;

import net.aqualoco.restrictiveclasses.core.RC;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RoleSyncS2CPayload(String roleId) implements CustomPayload {
    public static final CustomPayload.Id<RoleSyncS2CPayload> ID = new CustomPayload.Id<>(RC.id("role_sync"));
    public static final PacketCodec<RegistryByteBuf, RoleSyncS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, RoleSyncS2CPayload::roleId,
                    RoleSyncS2CPayload::new
            );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
