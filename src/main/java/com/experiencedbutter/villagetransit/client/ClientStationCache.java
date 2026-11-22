package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.network.StationDataPayload;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Last station snapshot received from the server, used by {@link VillageStationScreen}. */
public final class ClientStationCache {

    private static final Map<BlockPos, StationDataPayload> DATA = new HashMap<>();

    private ClientStationCache() {
    }

    public static void put(StationDataPayload payload) {
        DATA.put(payload.pos(), payload);
    }

    public static StationDataPayload get(BlockPos pos) {
        return DATA.get(pos);
    }

    public static void clear() {
        DATA.clear();
    }
}
