package com.experiencedbutter.villagetransit.client;

import net.minecraft.core.BlockPos;

/**
 * In-world block picking mode used by the platform lamp: the player clicks a slot, the screen
 * closes, and the next block they right click becomes the link target (same idea as choosing a
 * package port target in Create).
 */
public final class SelectionMode {

    private static BlockPos lampPos;
    private static int slot = -1;

    private SelectionMode() {
    }

    public static void start(BlockPos pos, int slotIndex) {
        lampPos = pos;
        slot = slotIndex;
    }

    public static boolean isActive() {
        return lampPos != null && slot >= 0;
    }

    public static BlockPos lampPos() {
        return lampPos;
    }

    public static int slot() {
        return slot;
    }

    public static void clear() {
        lampPos = null;
        slot = -1;
    }
}
