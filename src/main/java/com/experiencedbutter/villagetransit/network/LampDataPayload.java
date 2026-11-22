package com.experiencedbutter.villagetransit.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client state of a platform lamp. */
public record LampDataPayload(BlockPos pos,
                              boolean hasVillage,
                              BlockPos villagePos,
                              boolean hasStation,
                              BlockPos stationPos,
                              boolean lit,
                              int status) implements CustomPacketPayload {

    /** 0 = complete, 1 = village link missing, 2 = train station link missing, 3 = village inactive. */
    public static final int STATUS_COMPLETE = 0;
    public static final int STATUS_NO_VILLAGE = 1;
    public static final int STATUS_NO_STATION = 2;
    public static final int STATUS_VILLAGE_INACTIVE = 3;
    public static final int STATUS_STATION_MISSING = 4;

    public static final Type<LampDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("create_village_transit", "lamp_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LampDataPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                buffer.writeBoolean(payload.hasVillage());
                if (payload.hasVillage()) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.villagePos());
                }
                buffer.writeBoolean(payload.hasStation());
                if (payload.hasStation()) {
                    BlockPos.STREAM_CODEC.encode(buffer, payload.stationPos());
                }
                buffer.writeBoolean(payload.lit());
                buffer.writeVarInt(payload.status());
            },
            buffer -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
                boolean hasVillage = buffer.readBoolean();
                BlockPos village = hasVillage ? BlockPos.STREAM_CODEC.decode(buffer) : null;
                boolean hasStation = buffer.readBoolean();
                BlockPos station = hasStation ? BlockPos.STREAM_CODEC.decode(buffer) : null;
                boolean lit = buffer.readBoolean();
                int status = buffer.readVarInt();
                return new LampDataPayload(pos, hasVillage, village, hasStation, station, lit, status);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
