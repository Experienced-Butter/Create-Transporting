package com.experiencedbutter.villagetransit.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side state of a single village station. Stored in {@link VillageTransitData}, so it keeps
 * existing (and keeps ticking) while the chunk containing the block is unloaded.
 */
public final class StationRecord {

    public final BlockPos pos;
    public VillageStationState state = VillageStationState.UNBOUND;

    public int villagers;
    public int employed;
    public int workstations;
    public int beds;
    /** Identity of the village this station claims, derived from the structure start. */
    public String villageId = "";
    /** Machine readable reason of the last failed/last survey, shown in the GUI. */
    public String lastReason = "";
    /** Updates since the last patience decay, so decay can happen every N updates. */
    public int updatesSinceDecay;
    public boolean censusTruncated;
    public long lastCensusTick = -1L;

    public long nextCensusTick;
    public long nextUpdateTick;
    public boolean refreshInFlight;

    /** Travellers waiting at this station, oldest first. Item form, carrying their own components. */
    public final List<ItemStack> passengers = new ArrayList<>();

    /** Rolling window for the hourly payout cap. */
    public long payoutWindowStartTick;
    public long payoutWindowSum;

    public StationRecord(BlockPos pos) {
        this.pos = pos;
    }

    public static GlobalPos key(ResourceKey<Level> dimension, BlockPos pos) {
        return GlobalPos.of(dimension, pos.immutable());
    }

    public int unemployed() {
        return Math.max(0, villagers - employed);
    }

    public double unemployedRatio() {
        return villagers <= 0 ? 0.0 : (double) unemployed() / villagers;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        tag.putString("State", state.name());
        tag.putInt("Villagers", villagers);
        tag.putInt("Employed", employed);
        tag.putInt("Workstations", workstations);
        tag.putInt("Beds", beds);
        tag.putString("VillageId", villageId);
        tag.putString("LastReason", lastReason);
        tag.putInt("SinceDecay", updatesSinceDecay);
        tag.putBoolean("Truncated", censusTruncated);
        tag.putLong("LastCensus", lastCensusTick);
        tag.putLong("NextCensus", nextCensusTick);
        tag.putLong("NextUpdate", nextUpdateTick);
        tag.putLong("PayoutStart", payoutWindowStartTick);
        tag.putLong("PayoutSum", payoutWindowSum);
        ListTag list = new ListTag();
        for (ItemStack stack : passengers) {
            if (stack.isEmpty()) {
                continue;
            }
            list.add(stack.save(registries));
        }
        tag.put("Passengers", list);
        return tag;
    }

    public static StationRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        StationRecord record = new StationRecord(BlockPos.of(tag.getLong("Pos")));
        try {
            record.state = VillageStationState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException ignored) {
            record.state = VillageStationState.UNBOUND;
        }
        record.villagers = tag.getInt("Villagers");
        record.employed = tag.getInt("Employed");
        record.workstations = tag.getInt("Workstations");
        record.beds = tag.getInt("Beds");
        record.villageId = tag.getString("VillageId");
        record.lastReason = tag.getString("LastReason");
        record.updatesSinceDecay = tag.getInt("SinceDecay");
        record.censusTruncated = tag.getBoolean("Truncated");
        record.lastCensusTick = tag.getLong("LastCensus");
        record.nextCensusTick = tag.getLong("NextCensus");
        record.nextUpdateTick = tag.getLong("NextUpdate");
        record.payoutWindowStartTick = tag.getLong("PayoutStart");
        record.payoutWindowSum = tag.getLong("PayoutSum");
        ListTag list = tag.getList("Passengers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) {
                record.passengers.add(stack);
            }
        }
        return record;
    }
}
