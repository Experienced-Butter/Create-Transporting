package com.experiencedbutter.villagetransit.data;

import com.experiencedbutter.villagetransit.config.VTConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * World-wide state of the mod: village stations, platform lamps, train manifests, route fatigue
 * and wallets.
 *
 * <p>This is the reason stations and trains keep working while unloaded: block entities are only
 * facades, all long-lived data lives here and is saved with the overworld.</p>
 */
public final class VillageTransitData extends SavedData {

    private static final String DATA_NAME = "create_village_transit";
    private static final int DATA_VERSION = 1;

    public static final SavedData.Factory<VillageTransitData> FACTORY =
            new SavedData.Factory<>(VillageTransitData::new, VillageTransitData::load);

    private final Map<GlobalPos, StationRecord> stations = new LinkedHashMap<>();
    private final Map<GlobalPos, LampRecord> lamps = new LinkedHashMap<>();
    private final Map<UUID, TrainManifest> manifests = new LinkedHashMap<>();
    private final Map<UUID, Long> wallets = new LinkedHashMap<>();
    /** Key: "dim|posA|posB" with the two village positions sorted, unordered pair. */
    private final Map<String, double[]> routeFatigue = new LinkedHashMap<>();
    private final Map<String, Long> routeFatigueTick = new LinkedHashMap<>();

    public static VillageTransitData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    // ------------------------------------------------------------------ stations

    public StationRecord get(GlobalPos key) {
        return stations.get(key);
    }

    public StationRecord getOrCreate(ResourceKey<Level> dimension, BlockPos pos) {
        GlobalPos key = GlobalPos.of(dimension, pos.immutable());
        StationRecord existing = stations.get(key);
        if (existing != null) {
            return existing;
        }
        StationRecord record = new StationRecord(pos.immutable());
        stations.put(key, record);
        setDirty();
        return record;
    }

    public void remove(GlobalPos key) {
        if (stations.remove(key) != null) {
            setDirty();
        }
    }

    public Collection<Map.Entry<GlobalPos, StationRecord>> entries() {
        return stations.entrySet();
    }

    public int size() {
        return stations.size();
    }

    // ------------------------------------------------------------------ lamps

    public LampRecord getLamp(GlobalPos key) {
        return lamps.get(key);
    }

    public LampRecord getOrCreateLamp(ResourceKey<Level> dimension, BlockPos pos) {
        GlobalPos key = GlobalPos.of(dimension, pos.immutable());
        LampRecord existing = lamps.get(key);
        if (existing != null) {
            return existing;
        }
        LampRecord record = new LampRecord(pos.immutable());
        lamps.put(key, record);
        setDirty();
        return record;
    }

    public void removeLamp(GlobalPos key) {
        if (lamps.remove(key) != null) {
            setDirty();
        }
    }

    public Collection<Map.Entry<GlobalPos, LampRecord>> lampEntries() {
        return lamps.entrySet();
    }

    public LampRecord findLampForStation(UUID stationId) {
        for (LampRecord lamp : lamps.values()) {
            if (stationId.equals(lamp.trainStationId)) {
                return lamp;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ trains

    public TrainManifest manifest(UUID trainId) {
        TrainManifest manifest = manifests.get(trainId);
        if (manifest == null) {
            manifest = new TrainManifest();
            manifests.put(trainId, manifest);
        }
        return manifest;
    }

    public TrainManifest peekManifest(UUID trainId) {
        return manifests.get(trainId);
    }

    public void removeManifest(UUID trainId) {
        if (manifests.remove(trainId) != null) {
            setDirty();
        }
    }

    public Collection<Map.Entry<UUID, TrainManifest>> manifests() {
        return manifests.entrySet();
    }

    // ------------------------------------------------------------------ wallets

    public long balance(UUID player) {
        return wallets.getOrDefault(player, 0L);
    }

    public void setBalance(UUID player, long value) {
        wallets.put(player, value);
        setDirty();
    }

    /** Adds (or subtracts, allowing debt) money and clamps the upper bound. */
    public long addBalance(UUID player, long delta) {
        long next = balance(player) + delta;
        long max = VTConfig.get().maxBalance;
        if (next > max) {
            next = max;
        }
        wallets.put(player, next);
        setDirty();
        return next;
    }

    // ------------------------------------------------------------------ route fatigue

    private static String routeKey(GlobalPos a, GlobalPos b) {
        String ka = a.dimension().location() + "@" + a.pos().asLong();
        String kb = b.dimension().location() + "@" + b.pos().asLong();
        return ka.compareTo(kb) <= 0 ? ka + "|" + kb : kb + "|" + ka;
    }

    public double currentRouteFactor(GlobalPos a, GlobalPos b, long now, VTConfig.Snapshot cfg) {
        if (!cfg.routeFatigueEnabled || a.equals(b)) {
            return 1.0;
        }
        String key = routeKey(a, b);
        double[] state = routeFatigue.get(key);
        if (state == null) {
            return 1.0;
        }
        long last = routeFatigueTick.getOrDefault(key, now);
        double fatigue = state[0] * Math.pow(0.5, (now - last) / (cfg.routeFatigueHalfLifeSeconds * 20.0));
        return com.experiencedbutter.villagetransit.economy.Rewards.routeFactor(fatigue, cfg);
    }

    public void registerRouteArrival(GlobalPos a, GlobalPos b, long now, VTConfig.Snapshot cfg) {
        if (!cfg.routeFatigueEnabled || a.equals(b)) {
            return;
        }
        String key = routeKey(a, b);
        double[] state = routeFatigue.get(key);
        long last = routeFatigueTick.getOrDefault(key, now);
        double fatigue = state == null ? 0.0
                : state[0] * Math.pow(0.5, (now - last) / (cfg.routeFatigueHalfLifeSeconds * 20.0));
        routeFatigue.put(key, new double[] {fatigue + 1.0});
        routeFatigueTick.put(key, now);
        setDirty();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", DATA_VERSION);

        ListTag stationList = new ListTag();
        stations.forEach((key, record) -> {
            CompoundTag entry = record.save(registries);
            entry.putString("Dimension", key.dimension().location().toString());
            stationList.add(entry);
        });
        tag.put("Stations", stationList);

        ListTag lampList = new ListTag();
        lamps.forEach((key, record) -> {
            CompoundTag entry = record.save();
            entry.putString("Dimension", key.dimension().location().toString());
            lampList.add(entry);
        });
        tag.put("Lamps", lampList);

        ListTag manifestList = new ListTag();
        manifests.forEach((trainId, manifest) -> {
            if (!manifest.isEmpty()) {
                manifestList.add(manifest.save(registries, trainId));
            }
        });
        tag.put("Manifests", manifestList);

        ListTag walletList = new ListTag();
        wallets.forEach((player, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("Player", NbtUtils.createUUID(player));
            entry.putLong("Balance", amount);
            walletList.add(entry);
        });
        tag.put("Wallets", walletList);

        ListTag fatigueList = new ListTag();
        routeFatigue.forEach((key, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Pair", key);
            entry.putDouble("Fatigue", state[0]);
            entry.putLong("Tick", routeFatigueTick.getOrDefault(key, 0L));
            fatigueList.add(entry);
        });
        tag.put("RouteFatigue", fatigueList);

        return tag;
    }

    public static VillageTransitData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageTransitData data = new VillageTransitData();

        ListTag stationList = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < stationList.size(); i++) {
            CompoundTag entry = stationList.getCompound(i);
            ResourceKey<Level> dimension = dimension(entry.getString("Dimension"));
            if (dimension == null) {
                continue;
            }
            StationRecord record = StationRecord.load(entry, registries);
            data.stations.put(GlobalPos.of(dimension, record.pos), record);
        }

        ListTag lampList = tag.getList("Lamps", Tag.TAG_COMPOUND);
        for (int i = 0; i < lampList.size(); i++) {
            CompoundTag entry = lampList.getCompound(i);
            ResourceKey<Level> dimension = dimension(entry.getString("Dimension"));
            if (dimension == null) {
                continue;
            }
            LampRecord record = LampRecord.load(entry);
            data.lamps.put(GlobalPos.of(dimension, record.pos), record);
        }

        ListTag manifestList = tag.getList("Manifests", Tag.TAG_COMPOUND);
        for (int i = 0; i < manifestList.size(); i++) {
            CompoundTag entry = manifestList.getCompound(i);
            UUID trainId = TrainManifest.trainId(entry);
            if (trainId != null) {
                data.manifests.put(trainId, TrainManifest.load(entry, registries));
            }
        }

        ListTag walletList = tag.getList("Wallets", Tag.TAG_COMPOUND);
        for (int i = 0; i < walletList.size(); i++) {
            CompoundTag entry = walletList.getCompound(i);
            data.wallets.put(NbtUtils.loadUUID(entry.get("Player")), entry.getLong("Balance"));
        }

        ListTag fatigueList = tag.getList("RouteFatigue", Tag.TAG_COMPOUND);
        for (int i = 0; i < fatigueList.size(); i++) {
            CompoundTag entry = fatigueList.getCompound(i);
            data.routeFatigue.put(entry.getString("Pair"), new double[] {entry.getDouble("Fatigue")});
            data.routeFatigueTick.put(entry.getString("Pair"), entry.getLong("Tick"));
        }

        return data;
    }

    private static ResourceKey<Level> dimension(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : ResourceKey.create(Registries.DIMENSION, location);
    }
}
