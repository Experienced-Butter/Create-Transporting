package com.experiencedbutter.villagetransit.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

/**
 * A platform lamp links exactly one village station with exactly one Create train station.
 */
public final class LampRecord {

    public final BlockPos pos;
    public GlobalPos village;
    public GlobalPos trainStationPos;
    public UUID trainStationId;

    public LampRecord(BlockPos pos) {
        this.pos = pos;
    }

    public boolean hasVillage() {
        return village != null;
    }

    public boolean hasTrainStation() {
        return trainStationPos != null && trainStationId != null;
    }

    public boolean fullyLinked() {
        return hasVillage() && hasTrainStation();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        if (village != null) {
            tag.putString("VillageDim", village.dimension().location().toString());
            tag.putLong("VillagePos", village.pos().asLong());
        }
        if (trainStationPos != null) {
            tag.putString("StationDim", trainStationPos.dimension().location().toString());
            tag.putLong("StationPos", trainStationPos.pos().asLong());
        }
        if (trainStationId != null) {
            tag.put("StationId", NbtUtils.createUUID(trainStationId));
        }
        return tag;
    }

    public static LampRecord load(CompoundTag tag) {
        LampRecord record = new LampRecord(BlockPos.of(tag.getLong("Pos")));
        if (tag.contains("VillageDim")) {
            var dim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("VillageDim")));
            record.village = GlobalPos.of(dim, BlockPos.of(tag.getLong("VillagePos")));
        }
        if (tag.contains("StationDim")) {
            var dim = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("StationDim")));
            record.trainStationPos = GlobalPos.of(dim, BlockPos.of(tag.getLong("StationPos")));
        }
        if (tag.contains("StationId")) {
            record.trainStationId = NbtUtils.loadUUID(tag.get("StationId"));
        }
        return record;
    }
}
