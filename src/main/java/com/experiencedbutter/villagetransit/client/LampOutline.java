package com.experiencedbutter.villagetransit.client;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Remembers bound positions a player just saw, so they can be outlined for a few seconds. */
public final class LampOutline {

    private static final long VISIBLE_MILLIS = 5000L;

    public static BlockPos lamp;
    public static BlockPos village;
    public static BlockPos station;
    public static ResourceKey<Level> dimension;
    private static long shownAt;

    private LampOutline() {
    }

    public static void show(BlockPos lampPos, BlockPos villagePos, BlockPos stationPos, ResourceKey<Level> dim) {
        lamp = lampPos;
        village = villagePos;
        station = stationPos;
        dimension = dim;
        shownAt = System.currentTimeMillis();
    }

    public static boolean visible() {
        return lamp != null && System.currentTimeMillis() - shownAt < VISIBLE_MILLIS;
    }
}
