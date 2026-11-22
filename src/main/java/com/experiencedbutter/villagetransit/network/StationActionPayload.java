package com.experiencedbutter.villagetransit.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server request: {@code REQUEST} refreshes the open screen, {@code REFRESH} starts a
 * forced village survey.
 */
public record StationActionPayload(BlockPos pos, Action action) implements CustomPacketPayload {

    public enum Action {
        REQUEST,
        REFRESH
    }

    public static final Type<StationActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("create_village_transit", "station_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StationActionPayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal),
            StationActionPayload::action,
            StationActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
