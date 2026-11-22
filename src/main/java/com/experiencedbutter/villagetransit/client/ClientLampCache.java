package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.network.LampDataPayload;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class ClientLampCache {

    private static final Map<BlockPos, LampDataPayload> DATA = new HashMap<>();

    private ClientLampCache() {
    }

    public static void put(LampDataPayload payload) {
        DATA.put(payload.pos(), payload);
    }

    public static LampDataPayload get(BlockPos pos) {
        return DATA.get(pos);
    }

    public static void clear() {
        DATA.clear();
    }
}
