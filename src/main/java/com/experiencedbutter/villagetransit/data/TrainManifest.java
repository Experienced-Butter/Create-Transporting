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
    /** Seat layout captured when the carriages were loaded; unchanged until the train is dismantled. */
    private final java.util.List<SeatKey> layout = new java.util.ArrayList<>();

    public Map<SeatKey, ItemStack> seats() {
        return seats;
    }

    public java.util.List<SeatKey> layout() {
        return layout;
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
        ListTag layoutTag = new ListTag();
        layout.forEach(key -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Carriage", key.carriage());
            entry.putLong("Seat", key.local().asLong());
            layoutTag.add(entry);
        });
        tag.put("Layout", layoutTag);
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
        ListTag layoutTag = tag.getList("Layout", Tag.TAG_COMPOUND);
        for (int i = 0; i < layoutTag.size(); i++) {
            CompoundTag entry = layoutTag.getCompound(i);
            manifest.layout.add(new SeatKey(entry.getInt("Carriage"), BlockPos.of(entry.getLong("Seat"))));
        }
        return manifest;
    }
}
