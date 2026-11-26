package com.experiencedbutter.villagetransit.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Every number in the mod lives here so server owners can re-balance without touching code.
 *
 * <p>The spec is registered as a SERVER config (it is part of world balance, not of the client
 * install). Because a spec that has not been loaded yet throws on {@code get()}, all values are
 * baked into an immutable {@link Snapshot} once the config file is read.</p>
 */
public final class VTConfig {

    public enum VillageCheckMode {
        /** Structure start of {@code #minecraft:village} AND enough beds / workstations nearby. */
        STRUCTURE_AND_POI,
        /** Structure start only. */
        STRUCTURE_ONLY,
        /** Bed / workstation POIs only, no structure requirement (self-built villages). */
        POI_ONLY
    }

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ---------------------------------------------------------------- village
    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_TAGS = B
            .comment("Structure tags that count as a village. Default: minecraft:village")
            .defineList("village.structureTags", List.of("minecraft:village"),
                    () -> "minecraft:village", o -> o instanceof String);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_IDS = B
            .comment("Exact structure ids that count as a village, for modded villages (e.g. ctov:village_plains).")
            .defineList("village.structureIds", List.of(), () -> "", o -> o instanceof String);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_NAME_CONTAINS = B
            .comment("Match any structure whose id contains one of these strings; covers most modded villages.")
            .defineList("village.structureNameContains", List.of("village"), () -> "village", o -> o instanceof String);    private static final ModConfigSpec.BooleanValue KEEP_PREVIOUS_COUNTS = B
            .comment("A survey that only saw part of the area keeps the previous (higher) villager count instead of dropping villagers it could not see.")
            .define("village.keepPreviousCountsWhenTruncated", true);    private static final ModConfigSpec.EnumValue<VillageCheckMode> CHECK_MODE = B
            .comment("How a village station decides that it sits inside a village.",
                    "STRUCTURE_AND_POI: #minecraft:village structure start + nearby beds/workstations",
                    "STRUCTURE_ONLY: structure start only",
                    "POI_ONLY: beds/workstations only (player built villages)")
            .defineEnum("village.checkMode", VillageCheckMode.STRUCTURE_AND_POI);
    private static final ModConfigSpec.IntValue CENSUS_RADIUS = B
            .comment("Radius (blocks) around the station that is scanned for villagers and POIs.")
            .defineInRange("village.censusRadius", 64, 8, 128);
    private static final ModConfigSpec.IntValue STRUCTURE_MARGIN = B
            .comment("How far (blocks) the structure bounding box is expanded before scanning.")
            .defineInRange("village.structureMargin", 16, 0, 128);
    private static final ModConfigSpec.IntValue MIN_BEDS = B
            .comment("Minimum bed POIs inside the scan area for STRUCTURE_AND_POI / POI_ONLY.")
            .defineInRange("village.minBeds", 3, 0, 256);
    private static final ModConfigSpec.IntValue MIN_WORKSTATIONS = B
            .comment("Minimum workstation POIs inside the scan area.")
            .defineInRange("village.minWorkstations", 1, 0, 256);
    private static final ModConfigSpec.BooleanValue REQUIRE_FULLY_LOADED = B
            .comment("Only census when every chunk of the scan area is loaded.",
                    "Prevents under-counting a village whose far half is unloaded.")
            .define("village.requireFullyLoadedCensus", false);
    private static final ModConfigSpec.IntValue CENSUS_RETRY_TICKS = B
            .comment("Retry delay after a census was skipped because chunks were missing.")
            .defineInRange("village.censusRetryTicks", 200, 1, 24000);
    private static final ModConfigSpec.IntValue CENSUS_RETRY_MAX_TICKS = B
            .comment("Upper bound of the exponential back-off between census retries.")
            .defineInRange("village.censusRetryMaxTicks", 1200, 20, 24000);
    private static final ModConfigSpec.IntValue STALE_TIMEOUT_MINUTES = B
            .comment("After this many minutes without a successful census the station is marked stale.")
            .defineInRange("village.staleTimeoutMinutes", 30, 1, 1440);
    private static final ModConfigSpec.BooleanValue STOP_GENERATING_WHEN_STALE = B
            .comment("Stop creating travellers once the census is stale. Default false: keep using",
                    "the last known census so unloaded villages keep producing travellers.")
            .define("village.stopGeneratingWhenStale", false);
    private static final ModConfigSpec.BooleanValue INVALID_WHEN_NO_VILLAGERS = B
            .comment("A village station becomes invalid when the village has no villagers left.")
            .define("village.invalidWhenNoVillagers", true);
    private static final ModConfigSpec.IntValue MIN_STATION_SEPARATION = B
            .comment("Minimum distance (blocks, same dimension) between two village stations.")
            .defineInRange("village.minStationSeparation", 48, 0, 512);
    private static final ModConfigSpec.BooleanValue ONE_STATION_PER_VILLAGE = B
            .comment("Only one village station may claim a given village.")
            .define("village.oneStationPerVillage", true);

    // ---------------------------------------------------------------- station
    private static final ModConfigSpec.IntValue CENSUS_BASE_TICKS = B
            .comment("Base delay between two censuses (20 ticks = 1 second).")
            .defineInRange("station.censusBaseTicks", 1200, 200, 24000);
    private static final ModConfigSpec.IntValue CENSUS_PER_VILLAGER_TICKS = B
            .comment("Extra census delay per villager, so large villages are checked less often.")
            .defineInRange("station.censusPerVillagerTicks", 60, 0, 1200);
    private static final ModConfigSpec.IntValue CENSUS_MAX_TICKS = B
            .comment("Cap for the per-villager part of the census delay.")
            .defineInRange("station.censusMaxTicks", 4800, 0, 60000);
    private static final ModConfigSpec.IntValue CENSUS_JITTER_TICKS = B
            .comment("Random jitter added to the census delay.")
            .defineInRange("station.censusJitterTicks", 600, 0, 6000);
    private static final ModConfigSpec.IntValue PASSENGER_CAPACITY = B
            .comment("How many travellers a village station can hold at once.")
            .defineInRange("station.passengerCapacity", 64, 1, 512);
    private static final ModConfigSpec.IntValue REFRESH_COOLDOWN_TICKS = B
            .comment("Cooldown of the manual refresh button.")
            .defineInRange("station.refreshButtonCooldownTicks", 20, 0, 1200);
    private static final ModConfigSpec.BooleanValue LAMP_REQUIRES_ACTIVE_STATION = B
            .comment("A platform lamp only works while its village station is active.")
            .define("station.lampRequiresActiveStation", true);

    // ---------------------------------------------------------------- passenger
    private static final ModConfigSpec.DoubleValue TRAVEL_CHANCE = B
            .comment("Chance per valid villager that a traveller is created during a list update.")
            .defineInRange("passenger.travelChance", 0.10, 0.0, 1.0);
    private static final ModConfigSpec.IntValue PATIENCE_DECAY_EVERY = B
            .comment("How many list updates pass before waiting travellers lose one patience point.")
            .defineInRange("passenger.patienceDecayEveryUpdates", 1, 1, 100);
    private static final ModConfigSpec.IntValue MAX_PER_UPDATE = B
            .comment("Hard cap of new travellers per list update.")
            .defineInRange("passenger.maxPerUpdate", 8, 1, 64);
    private static final ModConfigSpec.IntValue UPDATE_MIN_TICKS = B
            .comment("Minimum delay between two list updates (ticks).")
            .defineInRange("passenger.updateMinTicks", 1200, 100, 24000);
    private static final ModConfigSpec.IntValue UPDATE_MAX_TICKS = B
            .comment("Maximum delay between two list updates (ticks).")
            .defineInRange("passenger.updateMaxTicks", 3600, 100, 24000);
    private static final ModConfigSpec.IntValue ULTRA_RARE_ONE_IN = B
            .comment("Ultra patient travellers appear with a 1/N chance. 0 disables them.")
            .defineInRange("passenger.ultraRareOneIn", 64, 0, 100000);
    private static final ModConfigSpec.IntValue ULTRA_CAP = B
            .comment("How many ultra patient travellers a station may hold.")
            .defineInRange("passenger.ultraCap", 2, 0, 64);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> PATIENCE_WEIGHTS = B
            .comment("Weights for patience 1..8. Must contain 8 positive numbers.")
            .defineList("passenger.patienceWeights", List.of(1, 4, 7, 9, 6, 3, 2, 1),
                    () -> 1, o -> o instanceof Number n && n.intValue() > 0);
    private static final ModConfigSpec.ConfigValue<List<? extends Double>> EMPLOYED_WEIGHTS = B
            .comment("Purpose weights for employed villagers: travel, relocate, trade.")
            .defineList("passenger.employedWeights", List.of(0.30, 0.10, 0.60),
                    () -> 0.30, o -> o instanceof Number n && n.doubleValue() >= 0);
    private static final ModConfigSpec.ConfigValue<List<? extends Double>> UNEMPLOYED_WEIGHTS = B
            .comment("Purpose weights for unemployed villagers: travel, relocate, trade.")
            .defineList("passenger.unemployedWeights", List.of(0.30, 0.60, 0.10),
                    () -> 0.30, o -> o instanceof Number n && n.doubleValue() >= 0);

    // ---------------------------------------------------------------- preference
    private static final ModConfigSpec.IntValue TRAVEL_BELOW = B
            .comment("Travel purpose is satisfied in villages with fewer villagers than this.")
            .defineInRange("preference.travelBelow", 15, 1, 200);
    private static final ModConfigSpec.IntValue TRADE_ABOVE = B
            .comment("Trade purpose is satisfied in villages with more villagers than this.")
            .defineInRange("preference.tradeAbove", 15, 1, 200);
    private static final ModConfigSpec.DoubleValue RELOCATE_MAX_UNEMPLOYED = B
            .comment("Relocate purpose is satisfied when the unemployed villager share is at most this.")
            .defineInRange("preference.relocateMaxUnemployedRatio", 0.25, 0.0, 1.0);

    // ---------------------------------------------------------------- reward
    private static final ModConfigSpec.IntValue BASE_TRAVEL = B
            .comment("Base reward (in cents, 10 = 1 copper coin) per purpose.")
            .defineInRange("reward.baseTravel", 20, 0, 100000);
    private static final ModConfigSpec.IntValue BASE_RELOCATE = B
            .defineInRange("reward.baseRelocate", 10, 0, 100000);
    private static final ModConfigSpec.IntValue BASE_TRADE = B
            .defineInRange("reward.baseTrade", 35, 0, 100000);
    private static final ModConfigSpec.DoubleValue PATIENCE_BASE = B
            .comment("Patience multiplier: base + perPoint * patience.")
            .defineInRange("reward.patienceBase", 0.60, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue PATIENCE_PER_POINT = B
            .defineInRange("reward.patiencePerPoint", 0.15, 0.0, 10.0);
    private static final ModConfigSpec.IntValue FREE_DISTANCE = B
            .comment("No distance bonus below this many blocks (anti short-shuttle rule).")
            .defineInRange("reward.freeDistance", 256, 0, 10000);
    private static final ModConfigSpec.IntValue DISTANCE_K = B
            .comment("Saturation constant of the distance curve.")
            .defineInRange("reward.distanceK", 256, 1, 10000);
    private static final ModConfigSpec.DoubleValue CAP_TRAVEL = B
            .comment("Maximum distance multiplier per purpose.")
            .defineInRange("reward.capTravel", 2.0, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue CAP_RELOCATE = B
            .defineInRange("reward.capRelocate", 1.0, 0.0, 10.0);
    private static final ModConfigSpec.DoubleValue CAP_TRADE = B
            .defineInRange("reward.capTrade", 1.5, 0.0, 10.0);
    private static final ModConfigSpec.IntValue CROSS_DIMENSION_DISTANCE = B
            .comment("Distance used when origin and destination are in different dimensions.")
            .defineInRange("reward.crossDimensionDistance", 4096, 0, 1000000);
    private static final ModConfigSpec.DoubleValue ACCIDENT_PENALTY = B
            .comment("Multiplier applied to the calculated payout when a train crashes / is dismantled.")
            .defineInRange("reward.accidentPenaltyFactor", 1.0, 0.0, 10.0);
    private static final ModConfigSpec.IntValue MAX_PAYOUT_PER_ARRIVAL = B
            .comment("Cap of the total payout of a single arrival. 0 disables the cap.")
            .defineInRange("reward.maxPayoutPerArrival", 4000, 0, 10000000);
    private static final ModConfigSpec.IntValue HOURLY_CAP_PER_STATION = B
            .comment("Safety cap of rewards per village station per hour. 0 disables the cap.")
            .defineInRange("reward.hourlyCapPerStation", 6000, 0, 100000000);
    private static final ModConfigSpec.BooleanValue ROUTE_FATIGUE_ENABLED = B
            .comment("Repeated runs between the same two villages pay less (anti farm).")
            .define("reward.routeFatigueEnabled", true);
    private static final ModConfigSpec.IntValue ROUTE_FATIGUE_HALF_LIFE = B
            .defineInRange("reward.routeFatigueHalfLifeSeconds", 1800, 30, 86400);
    private static final ModConfigSpec.DoubleValue ROUTE_FATIGUE_K = B
            .defineInRange("reward.routeFatigueK", 4.0, 0.1, 100.0);
    private static final ModConfigSpec.DoubleValue ROUTE_FATIGUE_FLOOR = B
            .defineInRange("reward.routeFatigueFloor", 0.2, 0.0, 1.0);

    // ---------------------------------------------------------------- lamp
    private static final ModConfigSpec.IntValue MAX_LINK_DISTANCE = B
            .comment("Maximum distance between a platform lamp and the blocks it links.")
            .defineInRange("lamp.maxLinkDistance", 30, 1, 128);
    private static final ModConfigSpec.BooleanValue ALLOW_SHARED_LINKS = B
            .comment("Allow several lamps to bind the same train station.")
            .define("lamp.allowSharedLinks", false);

    // ---------------------------------------------------------------- economy
    private static final ModConfigSpec.LongValue MAX_BALANCE = B
            .comment("Upper bound of a wallet. Inserts beyond it are rejected.")
            .defineInRange("economy.maxBalance", 9_000_000_000_000_000L, 0L, Long.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue WITHDRAW_MODE_DEFAULT = B
            .comment("Trade station starts in withdraw mode instead of deposit mode.")
            .define("economy.withdrawModeDefault", false);
    private static final ModConfigSpec.IntValue REFILL_INTERVAL_TICKS = B
            .defineInRange("economy.refillIntervalTicks", 20, 1, 1200);
    private static final ModConfigSpec.IntValue MAX_MINT_PER_REFILL = B
            .defineInRange("economy.maxMintPerRefill", 1, 1, 64);
    private static final ModConfigSpec.IntValue MAX_MANUAL_MINT_PER_SLOT = B
            .defineInRange("economy.maxManualMintPerSlot", 64, 1, 64);
    private static final ModConfigSpec.BooleanValue OWNER_ONLY_GUI = B
            .comment("Only the owner may open the trade station interface.")
            .define("economy.ownerOnlyGui", true);

    // ---------------------------------------------------------------- seat / debug
    private static final ModConfigSpec.BooleanValue BLOCK_PLAYER_SEAT_WHEN_OCCUPIED = B
            .comment("A passenger seat holding a traveller cannot be sat on by players.")
            .define("seat.blockPlayerSeatWhenOccupied", true);
    private static final ModConfigSpec.BooleanValue DROP_ON_NON_TRAIN_DISMANTLE = B
            .comment("Travellers of a non-train contraption are dropped when it is dismantled.")
            .define("seat.dropPassengersOnNonTrainDismantle", true);
    private static final ModConfigSpec.BooleanValue LOG_CENSUS = B.define("debug.logCensus", false);
    private static final ModConfigSpec.BooleanValue LOG_TRANSFERS = B.define("debug.logTransfers", false);
    private static final ModConfigSpec.BooleanValue LOG_ECONOMY = B.define("debug.logEconomy", false);

    static {
        SPEC = B.build();
    }

    private VTConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    /** Baked values; replaced whenever the config is loaded or reloaded. */
    public static Snapshot get() {
        return Snapshot.CURRENT;
    }

    public static void bake() {
        if (!SPEC.isLoaded()) {
            return;
        }
        Snapshot.CURRENT = new Snapshot();
    }

    public static final class Snapshot {
        static volatile Snapshot CURRENT = new Snapshot(true);

        public final VillageCheckMode checkMode;
        public final List<String> structureTags;
        public final List<String> structureIds;
        public final List<String> structureNameContains;
        public final int censusRadius;
        public final int structureMargin;
        public final int minBeds;
        public final int minWorkstations;
        public final boolean requireFullyLoadedCensus;
        public final boolean keepPreviousCountsWhenTruncated;
        public final int censusRetryTicks;
        public final int censusRetryMaxTicks;
        public final int staleTimeoutMinutes;
        public final boolean stopGeneratingWhenStale;
        public final boolean invalidWhenNoVillagers;
        public final int minStationSeparation;
        public final boolean oneStationPerVillage;

        public final int censusBaseTicks;
        public final int censusPerVillagerTicks;
        public final int censusMaxTicks;
        public final int censusJitterTicks;
        public final int passengerCapacity;
        public final int refreshButtonCooldownTicks;
        public final boolean lampRequiresActiveStation;

        public final double travelChance;
        public final int patienceDecayEveryUpdates;
        public final int maxPerUpdate;
        public final int updateMinTicks;
        public final int updateMaxTicks;
        public final int ultraRareOneIn;
        public final int ultraCap;
        public final int[] patienceWeights;
        public final double[] employedWeights;
        public final double[] unemployedWeights;

        public final int travelBelow;
        public final int tradeAbove;
        public final double relocateMaxUnemployedRatio;

        public final int baseTravel;
        public final int baseRelocate;
        public final int baseTrade;
        public final double patienceBase;
        public final double patiencePerPoint;
        public final int freeDistance;
        public final int distanceK;
        public final double capTravel;
        public final double capRelocate;
        public final double capTrade;
        public final int crossDimensionDistance;
        public final double accidentPenaltyFactor;
        public final int maxPayoutPerArrival;
        public final int hourlyCapPerStation;
        public final boolean routeFatigueEnabled;
        public final int routeFatigueHalfLifeSeconds;
        public final double routeFatigueK;
        public final double routeFatigueFloor;

        public final int maxLinkDistance;
        public final boolean allowSharedLinks;

        public final long maxBalance;
        public final boolean withdrawModeDefault;
        public final int refillIntervalTicks;
        public final int maxMintPerRefill;
        public final int maxManualMintPerSlot;
        public final boolean ownerOnlyGui;

        public final boolean blockPlayerSeatWhenOccupied;
        public final boolean dropOnNonTrainDismantle;
        public final boolean logCensus;
        public final boolean logTransfers;
        public final boolean logEconomy;

        /** Defaults-only instance, used before the config file is loaded. */
        Snapshot(boolean defaults) {
            checkMode = VillageCheckMode.STRUCTURE_AND_POI;
            structureTags = List.of("minecraft:village");
            structureIds = List.of();
            structureNameContains = List.of("village");
            censusRadius = 64;
            structureMargin = 16;
            minBeds = 3;
            minWorkstations = 1;
            requireFullyLoadedCensus = false;
            keepPreviousCountsWhenTruncated = true;
            censusRetryTicks = 200;
            censusRetryMaxTicks = 1200;
            staleTimeoutMinutes = 30;
            stopGeneratingWhenStale = false;
            invalidWhenNoVillagers = true;
            minStationSeparation = 48;
            oneStationPerVillage = true;

            censusBaseTicks = 1200;
            censusPerVillagerTicks = 60;
            censusMaxTicks = 4800;
            censusJitterTicks = 600;
            passengerCapacity = 64;
            refreshButtonCooldownTicks = 20;
            lampRequiresActiveStation = true;

            travelChance = 0.10;
            patienceDecayEveryUpdates = 1;
            maxPerUpdate = 8;
            updateMinTicks = 1200;
            updateMaxTicks = 3600;
            ultraRareOneIn = 64;
            ultraCap = 2;
            patienceWeights = new int[] {1, 4, 7, 9, 6, 3, 2, 1};
            employedWeights = new double[] {0.30, 0.10, 0.60};
            unemployedWeights = new double[] {0.30, 0.60, 0.10};

            travelBelow = 15;
            tradeAbove = 15;
            relocateMaxUnemployedRatio = 0.25;

            baseTravel = 20;
            baseRelocate = 10;
            baseTrade = 35;
            patienceBase = 0.60;
            patiencePerPoint = 0.15;
            freeDistance = 256;
            distanceK = 256;
            capTravel = 2.0;
            capRelocate = 1.0;
            capTrade = 1.5;
            crossDimensionDistance = 4096;
            accidentPenaltyFactor = 1.0;
            maxPayoutPerArrival = 4000;
            hourlyCapPerStation = 6000;
            routeFatigueEnabled = true;
            routeFatigueHalfLifeSeconds = 1800;
            routeFatigueK = 4.0;
            routeFatigueFloor = 0.2;

            maxLinkDistance = 30;
            allowSharedLinks = false;

            maxBalance = 9_000_000_000_000_000L;
            withdrawModeDefault = false;
            refillIntervalTicks = 20;
            maxMintPerRefill = 1;
            maxManualMintPerSlot = 64;
            ownerOnlyGui = true;

            blockPlayerSeatWhenOccupied = true;
            dropOnNonTrainDismantle = true;
            logCensus = false;
            logTransfers = false;
            logEconomy = false;
        }

        /** Reads the loaded spec. */
        Snapshot() {
            checkMode = CHECK_MODE.get();
            structureTags = List.copyOf(STRUCTURE_TAGS.get());
            structureIds = List.copyOf(STRUCTURE_IDS.get());
            structureNameContains = List.copyOf(STRUCTURE_NAME_CONTAINS.get());
            censusRadius = CENSUS_RADIUS.get();
            structureMargin = STRUCTURE_MARGIN.get();
            minBeds = MIN_BEDS.get();
            minWorkstations = MIN_WORKSTATIONS.get();
            requireFullyLoadedCensus = REQUIRE_FULLY_LOADED.get();
            keepPreviousCountsWhenTruncated = KEEP_PREVIOUS_COUNTS.get();
            censusRetryTicks = CENSUS_RETRY_TICKS.get();
            censusRetryMaxTicks = CENSUS_RETRY_MAX_TICKS.get();
            staleTimeoutMinutes = STALE_TIMEOUT_MINUTES.get();
            stopGeneratingWhenStale = STOP_GENERATING_WHEN_STALE.get();
            invalidWhenNoVillagers = INVALID_WHEN_NO_VILLAGERS.get();
            minStationSeparation = MIN_STATION_SEPARATION.get();
            oneStationPerVillage = ONE_STATION_PER_VILLAGE.get();

            censusBaseTicks = CENSUS_BASE_TICKS.get();
            censusPerVillagerTicks = CENSUS_PER_VILLAGER_TICKS.get();
            censusMaxTicks = CENSUS_MAX_TICKS.get();
            censusJitterTicks = CENSUS_JITTER_TICKS.get();
            passengerCapacity = PASSENGER_CAPACITY.get();
            refreshButtonCooldownTicks = REFRESH_COOLDOWN_TICKS.get();
            lampRequiresActiveStation = LAMP_REQUIRES_ACTIVE_STATION.get();

            travelChance = TRAVEL_CHANCE.get();
            patienceDecayEveryUpdates = PATIENCE_DECAY_EVERY.get();
            maxPerUpdate = MAX_PER_UPDATE.get();
            updateMinTicks = UPDATE_MIN_TICKS.get();
            updateMaxTicks = Math.max(UPDATE_MIN_TICKS.get(), UPDATE_MAX_TICKS.get());
            ultraRareOneIn = ULTRA_RARE_ONE_IN.get();
            ultraCap = ULTRA_CAP.get();
            patienceWeights = readIntWeights(PATIENCE_WEIGHTS.get(), 8);
            employedWeights = readDoubleWeights(EMPLOYED_WEIGHTS.get(), 3);
            unemployedWeights = readDoubleWeights(UNEMPLOYED_WEIGHTS.get(), 3);

            travelBelow = TRAVEL_BELOW.get();
            tradeAbove = TRADE_ABOVE.get();
            relocateMaxUnemployedRatio = RELOCATE_MAX_UNEMPLOYED.get();

            baseTravel = BASE_TRAVEL.get();
            baseRelocate = BASE_RELOCATE.get();
            baseTrade = BASE_TRADE.get();
            patienceBase = PATIENCE_BASE.get();
            patiencePerPoint = PATIENCE_PER_POINT.get();
            freeDistance = FREE_DISTANCE.get();
            distanceK = Math.max(1, DISTANCE_K.get());
            capTravel = CAP_TRAVEL.get();
            capRelocate = CAP_RELOCATE.get();
            capTrade = CAP_TRADE.get();
            crossDimensionDistance = CROSS_DIMENSION_DISTANCE.get();
            accidentPenaltyFactor = ACCIDENT_PENALTY.get();
            maxPayoutPerArrival = MAX_PAYOUT_PER_ARRIVAL.get();
            hourlyCapPerStation = HOURLY_CAP_PER_STATION.get();
            routeFatigueEnabled = ROUTE_FATIGUE_ENABLED.get();
            routeFatigueHalfLifeSeconds = ROUTE_FATIGUE_HALF_LIFE.get();
            routeFatigueK = Math.max(0.1, ROUTE_FATIGUE_K.get());
            routeFatigueFloor = ROUTE_FATIGUE_FLOOR.get();

            maxLinkDistance = MAX_LINK_DISTANCE.get();
            allowSharedLinks = ALLOW_SHARED_LINKS.get();

            maxBalance = MAX_BALANCE.get();
            withdrawModeDefault = WITHDRAW_MODE_DEFAULT.get();
            refillIntervalTicks = REFILL_INTERVAL_TICKS.get();
            maxMintPerRefill = MAX_MINT_PER_REFILL.get();
            maxManualMintPerSlot = MAX_MANUAL_MINT_PER_SLOT.get();
            ownerOnlyGui = OWNER_ONLY_GUI.get();

            blockPlayerSeatWhenOccupied = BLOCK_PLAYER_SEAT_WHEN_OCCUPIED.get();
            dropOnNonTrainDismantle = DROP_ON_NON_TRAIN_DISMANTLE.get();
            logCensus = LOG_CENSUS.get();
            logTransfers = LOG_TRANSFERS.get();
            logEconomy = LOG_ECONOMY.get();
        }

        private static int[] readIntWeights(List<? extends Integer> list, int expected) {
            int[] out = new int[expected];
            for (int i = 0; i < expected; i++) {
                Integer v = i < list.size() ? list.get(i) : null;
                out[i] = v == null || v <= 0 ? 1 : v;
            }
            return out;
        }

        private static double[] readDoubleWeights(List<? extends Double> list, int expected) {
            double[] out = new double[expected];
            double sum = 0;
            for (int i = 0; i < expected; i++) {
                Double v = i < list.size() ? list.get(i) : null;
                out[i] = v == null || v < 0 ? 0 : v;
                sum += out[i];
            }
            if (sum <= 0) {
                out[0] = 1;
                return out;
            }
            for (int i = 0; i < expected; i++) {
                out[i] /= sum;
            }
            return out;
        }
    }
}
