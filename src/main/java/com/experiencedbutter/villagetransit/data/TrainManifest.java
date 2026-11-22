package com.experiencedbutter.villagetransit.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Travellers currently riding a train. Kept per seat so a train that gets unloaded, reloaded or
 * reassembled keeps its passengers; seats that disappear are cleaned up on the next arrival.
 */
public final class TrainManifest {

    public record SeatKey(int carriage, BlockPos local) {
    }

    private final Map<SeatKey, ItemStack> seats = new LinkedHashMap<>();

    public Map<SeatKey, ItemStack> seats() {
        return seats;
    }

    public boolean isEmpty() {
        return seats.isEmpty();
    }

    public int size() {
        return seats.size();
    }

    public CompoundTag save(HolderLookup.Provider registries, UUID trainId) {
        CompoundTag tag = new CompoundTag();
        tag.put("Train", NbtUtils.createUUID(trainId));
        ListTag list = new ListTag();
        seats.forEach((key, stack) -> {
            if (stack.isEmpty()) {
                return;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Carriage", key.carriage());
            entry.putLong("Seat", key.local().asLong());
            entry.put("Stack", stack.save(registries));
            list.add(entry);
        });
        tag.put("Seats", list);
        return tag;
    }

    public static UUID trainId(CompoundTag tag) {
        return NbtUtils.loadUUID(tag.get("Train"));
    }

    public static TrainManifest load(CompoundTag tag, HolderLookup.Provider registries) {
        TrainManifest manifest = new TrainManifest();
        ListTag list = tag.getList("Seats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
            if (stack.isEmpty()) {
                continue;
            }
            manifest.seats.put(new SeatKey(entry.getInt("Carriage"), BlockPos.of(entry.getLong("Seat"))), stack);
        }
        return manifest;
    }
}
