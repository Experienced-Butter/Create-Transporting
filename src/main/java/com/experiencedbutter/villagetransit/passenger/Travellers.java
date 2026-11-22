package com.experiencedbutter.villagetransit.passenger;

import com.experiencedbutter.villagetransit.registry.ModDataComponents;
import com.experiencedbutter.villagetransit.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * Factory and ageing helpers for the traveller item. The item itself is the source of truth for
 * purpose, patience and origin; station records simply hold a list of these stacks.
 */
public final class Travellers {

    private Travellers() {
    }

    /** Marks a seat as occupied by a player (no traveller data, so it is never treated as one). */
    public static ItemStack marker(String playerName) {
        ItemStack stack = new ItemStack(ModItems.SEAT_MARKER.get());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(playerName));
        return stack;
    }

    public static boolean isMarker(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.SEAT_MARKER.get());
    }

    public static ItemStack create(PassengerPurpose purpose, int patience, GlobalPos origin, long bornTick) {
        ItemStack stack = new ItemStack(ModItems.PASSENGER.get());
        stack.set(ModDataComponents.PASSENGER.get(), new PassengerData(purpose, patience, origin, bornTick));
        return stack;
    }

    /** Human readable origin, derived from the stored position (no duplicated text in the item). */
    public static String originText(PassengerData data) {
        if (data == null || data.origin() == null) {
            return "";
        }
        BlockPos pos = data.origin().pos();
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static boolean isTraveller(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModDataComponents.PASSENGER.get());
    }

    public static PassengerData data(ItemStack stack) {
        return stack.get(ModDataComponents.PASSENGER.get());
    }

    /**
     * Ages a traveller while it waits at a station: ultra patient travellers keep their patience,
     * everyone else loses one point and is removed at zero.
     *
     * @return the aged stack, or {@code null} if the traveller gave up
     */
    public static ItemStack ageWaiting(ItemStack stack) {
        PassengerData data = data(stack);
        if (data == null || data.isUltra()) {
            return stack;
        }
        int next = data.patience() - 1;
        if (next <= 0) {
            return null;
        }
        ItemStack copy = stack.copyWithCount(1);
        copy.set(ModDataComponents.PASSENGER.get(), data.withPatience(next));
        return copy;
    }

    /**
     * Ages a traveller on board a train: every station costs one patience point, ultra patient
     * travellers included. Returns {@code null} once patience reaches zero.
     */
    public static ItemStack ageOnBoard(ItemStack stack) {
        PassengerData data = data(stack);
        if (data == null) {
            return stack;
        }
        int next = data.patience() - 1;
        if (next <= 0) {
            return null;
        }
        ItemStack copy = stack.copyWithCount(1);
        copy.set(ModDataComponents.PASSENGER.get(), data.withPatience(next));
        return copy;
    }
}
