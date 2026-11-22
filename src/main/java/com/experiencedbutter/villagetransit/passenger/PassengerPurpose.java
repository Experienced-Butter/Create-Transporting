package com.experiencedbutter.villagetransit.passenger;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum PassengerPurpose implements StringRepresentable {
    /** Wants a small village. */
    TRAVEL("travel"),
    /** Wants a village with few unemployed villagers. */
    RELOCATE("relocate"),
    /** Wants a big village. */
    TRADE("trade");

    public static final Codec<PassengerPurpose> CODEC = StringRepresentable.fromEnum(PassengerPurpose::values);
    public static final StreamCodec<ByteBuf, PassengerPurpose> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], PassengerPurpose::ordinal);

    private final String name;

    PassengerPurpose(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getDescriptionId() {
        return "purpose.create_village_transit." + name;
    }
}
