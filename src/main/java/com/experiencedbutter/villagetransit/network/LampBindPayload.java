package com.experiencedbutter.villagetransit.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client asks the server to bind lamp slot {@code slot} (0 village, 1 train station) to a block. */
public record LampBindPayload(BlockPos lampPos, int slot, BlockPos target) implements CustomPacketPayload {

    public static final Type<LampBindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("create_village_transit", "lamp_bind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LampBindPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LampBindPayload::lampPos,
            ByteBufCodecs.VAR_INT, LampBindPayload::slot,
            BlockPos.STREAM_CODEC, LampBindPayload::target,
            LampBindPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
