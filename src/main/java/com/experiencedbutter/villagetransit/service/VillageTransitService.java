package com.experiencedbutter.villagetransit.service;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageStationState;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.passenger.PassengerPurpose;
import com.experiencedbutter.villagetransit.village.VillageCensus;
import com.experiencedbutter.villagetransit.village.VillageStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Global station scheduler. Runs from the server tick, not from block entities, so stations keep
 * being simulated while their chunk is unloaded.
 *
 * <p>At most one census is executed per tick (staggered), and only when the station is due.</p>
 */
public final class VillageTransitService {

    private static final Random RANDOM = new Random();

    private VillageTransitService() {
    }

    private static long lastTickNanos;
    private static int lastStationCount;

    public static long lastTickNanos() {
        return lastTickNanos;
    }

    public static int lastStationCount() {
        return lastStationCount;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        long startedAt = System.nanoTime();
        MinecraftServer server = event.getServer();
        VillageTransitData data = VillageTransitData.get(server);
        long now = server.overworld().getGameTime();
        int censusBudget = 1;
        boolean dirty = false;
        List<GlobalPos> broken = null;

        for (var entry : new ArrayList<>(data.entries())) {
            GlobalPos key = entry.getKey();
            StationRecord record = entry.getValue();
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                broken = add(broken, key);
                continue;
            }
            if (!level.hasChunk(key.pos().getX() >> 4, key.pos().getZ() >> 4)) {
                // Unloaded: keep simulating from the saved record, but never force the chunk in.
                if (now >= record.nextUpdateTick) {
                    updateTravellerList(server, level, key, record, now);
                    dirty = true;
                }
                continue;
            }
            if (!level.getBlockState(key.pos()).is(com.experiencedbutter.villagetransit.registry.ModBlocks.VILLAGE_STATION.get())) {
                broken = add(broken, key);
                continue;
            }

            if (now >= record.nextUpdateTick) {
                updateTravellerList(server, level, key, record, now);
                dirty = true;
            }
            if (now >= record.nextCensusTick && censusBudget > 0) {
                censusBudget--;
                runCensus(server, level, key, record, now);
                dirty = true;
            }
        }

        if (broken != null) {
            for (GlobalPos key : broken) {
                data.remove(key);
            }
        }

        com.experiencedbutter.villagetransit.train.TrainTracker.tick(server, data);
        if (dirty) {
            data.setDirty();
        }
        lastTickNanos = System.nanoTime() - startedAt;
        lastStationCount = data.size();
    }

    private static List<GlobalPos> add(List<GlobalPos> list, GlobalPos key) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(key);
        return list;
    }

    /** Forces an immediate census (the GUI refresh button), respecting the in-flight flag. */
    public static void requestRefresh(MinecraftServer server, GlobalPos key, StationRecord record) {
        if (record.refreshInFlight) {
            return;
        }
        record.refreshInFlight = true;
        record.nextCensusTick = server.overworld().getGameTime();
    }

    private static void runCensus(MinecraftServer server, ServerLevel level, GlobalPos key,
                                  StationRecord record, long now) {
        VTConfig.Snapshot cfg = VTConfig.get();
        VillageCensus.Result result = VillageCensus.scan(level, record.pos);
        record.refreshInFlight = false;

        if (result.incomplete()) {
            record.lastReason = result.reason();
            // Keep the previous numbers; retry later with a mild back-off.
            long retry = Math.min(cfg.censusRetryMaxTicks, cfg.censusRetryTicks * 2L);
            record.nextCensusTick = now + retry;
            if (record.state == VillageStationState.ACTIVE && isStale(cfg, record, now)) {
                record.state = VillageStationState.STALE;
            }
            syncBlockState(level, key.pos(), record);
            return;
        }

        record.villagers = result.villagers();
        record.employed = result.employed();
        record.workstations = result.workstations();
        record.beds = result.beds();
        record.villageId = result.villageId();
        record.censusTruncated = result.incomplete();
        record.lastCensusTick = now;

        record.lastReason = result.valid() ? "" : result.reason();
        if (!result.valid()) {
            record.state = VillageStationState.INVALID;
        } else if (cfg.invalidWhenNoVillagers && result.villagers() == 0) {
            record.state = VillageStationState.INVALID;
        } else {
            record.state = VillageStationState.ACTIVE;
        }

        record.nextCensusTick = now + nextCensusDelay(cfg, result.villagers());
        if (record.nextUpdateTick <= 0L) {
            record.nextUpdateTick = now + nextUpdateDelay(cfg);
        }
        syncBlockState(level, key.pos(), record);

        if (cfg.logCensus) {
            CreateVillageTransit.LOGGER.info("[census] {} -> {} villagers={} employed={} workstations={} beds={}",
                    key.pos(), record.state, record.villagers, record.employed, record.workstations, record.beds);
        }
    }

    private static void updateTravellerList(MinecraftServer server, ServerLevel level, GlobalPos key,
                                            StationRecord record, long now) {
        VTConfig.Snapshot cfg = VTConfig.get();
        // Ageing can be spread over several updates so patience falls slower than the list churns.
        record.updatesSinceDecay++;
        if (record.updatesSinceDecay >= cfg.patienceDecayEveryUpdates) {
            record.updatesSinceDecay = 0;
            if (!record.passengers.isEmpty()) {
                List<ItemStack> agedList = new ArrayList<>(record.passengers.size());
                for (ItemStack stack : record.passengers) {
                    ItemStack aged = com.experiencedbutter.villagetransit.passenger.Travellers.ageWaiting(stack);
                    if (aged != null) {
                        agedList.add(aged);
                    }
                }
                // Always write back: without this, patience only dropped when someone was removed.
                record.passengers.clear();
                record.passengers.addAll(agedList);
            }
        }

        generateTravellers(key, record, now, cfg);
        record.nextUpdateTick = now + nextUpdateDelay(cfg);

        if (cfg.logCensus) {
            CreateVillageTransit.LOGGER.info("[travellers] {} list={}", key.pos(), record.passengers.size());
        }
    }

    private static void generateTravellers(GlobalPos key, StationRecord record, long now, VTConfig.Snapshot cfg) {
        if (record.state != VillageStationState.ACTIVE || record.villagers <= 0) {
            return;
        }
        if (cfg.stopGeneratingWhenStale && record.lastCensusTick >= 0
                && now - record.lastCensusTick > cfg.staleTimeoutMinutes * 60L * 20L) {
            return;
        }
        int room = cfg.passengerCapacity - record.passengers.size();
        if (room <= 0) {
            return;
        }

        int count = com.experiencedbutter.villagetransit.economy.TravellerMath
                .expectedCount(cfg.travelChance, record.villagers, RANDOM.nextDouble());
        count = Math.min(count, Math.min(cfg.maxPerUpdate, room));
        if (count <= 0) {
            return;
        }

        int ultraWaiting = 0;
        for (ItemStack stack : record.passengers) {
            var data = com.experiencedbutter.villagetransit.passenger.Travellers.data(stack);
            if (data != null && data.isUltra()) {
                ultraWaiting++;
            }
        }

        for (int i = 0; i < count; i++) {
            int patience = rollPatience(cfg);
            if (patience >= com.experiencedbutter.villagetransit.passenger.PassengerData.ULTRA_PATIENCE) {
                if (ultraWaiting >= cfg.ultraCap) {
                    // Silently drop the ultra patient traveller instead of re-rolling.
                    continue;
                }
                ultraWaiting++;
            }
            PassengerPurpose purpose = rollPurpose(record, cfg);
            record.passengers.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                    purpose, patience, key, now));
        }
    }

    /** Weighted purpose roll, merged from the employed and unemployed weight tables. */
    public static PassengerPurpose rollPurpose(StationRecord record, VTConfig.Snapshot cfg) {
        double[] probabilities = com.experiencedbutter.villagetransit.economy.TravellerMath
                .purposeProbabilities(record.villagers, record.employed, cfg.employedWeights, cfg.unemployedWeights);
        int index = com.experiencedbutter.villagetransit.economy.TravellerMath
                .pickPurpose(RANDOM.nextDouble(), probabilities);
        return PassengerPurpose.values()[Math.min(index, PassengerPurpose.values().length - 1)];
    }

    /** Table lookup for patience 1..8, plus a rare ultra patient traveller. */
    public static int rollPatience(VTConfig.Snapshot cfg) {
        if (cfg.ultraRareOneIn > 0 && RANDOM.nextInt(cfg.ultraRareOneIn) == 0) {
            return com.experiencedbutter.villagetransit.passenger.PassengerData.ULTRA_PATIENCE;
        }
        return com.experiencedbutter.villagetransit.economy.TravellerMath
                .pickPatience(RANDOM.nextDouble(), cfg.patienceWeights);
    }

    public static long nextCensusDelay(VTConfig.Snapshot cfg, int villagers) {
        long scaled = Math.min(cfg.censusMaxTicks, (long) cfg.censusPerVillagerTicks * Math.max(0, villagers));
        long jitter = cfg.censusJitterTicks <= 0 ? 0 : RANDOM.nextInt(cfg.censusJitterTicks + 1);
        return cfg.censusBaseTicks + scaled + jitter;
    }

    public static long nextUpdateDelay(VTConfig.Snapshot cfg) {
        int span = Math.max(1, cfg.updateMaxTicks - cfg.updateMinTicks);
        return cfg.updateMinTicks + RANDOM.nextInt(span + 1);
    }

    private static boolean isStale(VTConfig.Snapshot cfg, StationRecord record, long now) {
        if (record.lastCensusTick < 0L) {
            return false;
        }
        return now - record.lastCensusTick > cfg.staleTimeoutMinutes * 60L * 20L;
    }

    private static void syncBlockState(ServerLevel level, BlockPos pos, StationRecord record) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof VillageStationBlock)) {
            return;
        }
        boolean active = record.state == VillageStationState.ACTIVE;
        if (state.getValue(VillageStationBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(VillageStationBlock.ACTIVE, active), 3);
        }
    }
}
