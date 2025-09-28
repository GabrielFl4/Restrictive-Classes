package net.aqualoco.restrictiveclasses.network.payload;

import net.aqualoco.restrictiveclasses.core.RC;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RulesSyncS2CPayload(long version, String rolesJsonArray) implements CustomPayload {
    public static final CustomPayload.Id<RulesSyncS2CPayload> ID = new CustomPayload.Id<>(RC.id("rules_sync"));
    public static final PacketCodec<RegistryByteBuf, RulesSyncS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.LONG, RulesSyncS2CPayload::version,
                    PacketCodecs.STRING, RulesSyncS2CPayload::rolesJsonArray,
                    RulesSyncS2CPayload::new
            );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
