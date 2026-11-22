package com.experiencedbutter.villagetransit.passenger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything a traveller carries. Patience {@link #ULTRA_PATIENCE} marks an ultra patient
 * traveller: they never expire while waiting at a station.
 *
 * <p>Only the origin position is stored; the display text is derived, which keeps saved travellers
 * small.</p>
 */
public record PassengerData(PassengerPurpose purpose,
                            int patience,
                            GlobalPos origin,
                            long bornTick) {

    public static final int ULTRA_PATIENCE = 10;

    public static final Codec<PassengerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PassengerPurpose.CODEC.fieldOf("purpose").forGetter(PassengerData::purpose),
            Codec.INT.fieldOf("patience").forGetter(PassengerData::patience),
            GlobalPos.CODEC.fieldOf("origin").forGetter(PassengerData::origin),
            Codec.LONG.optionalFieldOf("born", 0L).forGetter(PassengerData::bornTick)
    ).apply(instance, PassengerData::new));

    public static final StreamCodec<ByteBuf, PassengerData> STREAM_CODEC = StreamCodec.composite(
            PassengerPurpose.STREAM_CODEC, PassengerData::purpose,
            ByteBufCodecs.VAR_INT, PassengerData::patience,
            GlobalPos.STREAM_CODEC, PassengerData::origin,
            ByteBufCodecs.VAR_LONG, PassengerData::bornTick,
            PassengerData::new);

    public boolean isUltra() {
        return patience >= ULTRA_PATIENCE;
    }

    public PassengerData withPatience(int newPatience) {
        return new PassengerData(purpose, newPatience, origin, bornTick);
    }
}
