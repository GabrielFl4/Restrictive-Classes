package net.aqualoco.restrictiveclasses.network.payload;

import net.aqualoco.restrictiveclasses.core.RC;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SelectRoleC2SPayload(String roleId) implements CustomPayload {
    public static final CustomPayload.Id<SelectRoleC2SPayload> ID =
            new CustomPayload.Id<>(RC.id("select_role"));
    public static final PacketCodec<RegistryByteBuf, SelectRoleC2SPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, SelectRoleC2SPayload::roleId, SelectRoleC2SPayload::new);

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}