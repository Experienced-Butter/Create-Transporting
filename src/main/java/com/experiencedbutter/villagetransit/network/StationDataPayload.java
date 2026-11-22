package com.experiencedbutter.villagetransit.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server to client snapshot of a village station, sent while its screen is open.
 */
public record StationDataPayload(BlockPos pos,
                                 int state,
                                 int villagers,
                                 int employed,
                                 int workstations,
                                 int beds,
                                 int secondsToCensus,
                                 int secondsToUpdate,
                                 boolean refreshInFlight,
                                 int capacity,
                                 String reason,
                                 long payoutWindowSum,
                                 int hourlyCap,
                                 List<ItemStack> passengers) implements CustomPacketPayload {

    public static final Type<StationDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("create_village_transit", "station_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationDataPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                buffer.writeVarInt(payload.state());
                buffer.writeVarInt(payload.villagers());
                buffer.writeVarInt(payload.employed());
                buffer.writeVarInt(payload.workstations());
                buffer.writeVarInt(payload.beds());
                buffer.writeVarInt(Math.max(0, payload.secondsToCensus()));
                buffer.writeVarInt(Math.max(0, payload.secondsToUpdate()));
                buffer.writeBoolean(payload.refreshInFlight());
                buffer.writeVarInt(Math.max(0, payload.capacity()));
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, payload.reason());
                buffer.writeVarLong(payload.payoutWindowSum());
                buffer.writeVarInt(Math.max(0, payload.hourlyCap()));
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buffer, payload.passengers());
            },
            buffer -> new StationDataPayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readVarLong(),
                    buffer.readVarInt(),
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
