package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.LampRecord;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.data.VillageStationState;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.economy.Rewards;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.PassengerSeatBlock;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Observes Create's trains without touching them: arrival, crash and dismantle are all derived
 * from the public railway manager, which keeps the add-on free of mixins.
 */
public final class TrainTracker {

    private static final Map<UUID, UUID> LAST_STATION = new HashMap<>();
    private static final Map<UUID, Boolean> LAST_DERAILED = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> LAST_DIM = new HashMap<>();
    /** First tick after (re)start only records the current state, so trains sitting at a station
     *  are not paid out again. */
    private static boolean primed;
    /** Last seat layout seen while the carriage was loaded; used when the train is far away. */
    private static final Map<UUID, List<TrainManifest.SeatKey>> LAST_SEATS = new java.util.concurrent.ConcurrentHashMap<>();

    private TrainTracker() {
    }

    /** Called when a server stops so no stale train ids leak into the next world. */
    public static void reset() {
        LAST_STATION.clear();
        LAST_DERAILED.clear();
        LAST_POS.clear();
        LAST_DIM.clear();
        primed = false;
    }

    public static void tick(MinecraftServer server, VillageTransitData data) {
        VTConfig.Snapshot cfg = VTConfig.get();
        long now = server.overworld().getGameTime();
        Set<UUID> seen = new HashSet<>();
        boolean firstTick = !primed;
        primed = true;

        for (Train train : new ArrayList<>(Create.RAILWAYS.trains.values())) {
            seen.add(train.id);
            GlobalStation station = safeCurrentStation(train);
            UUID stationId = station == null ? null : station.id;
            UUID previous = LAST_STATION.put(train.id, stationId);

            if (!firstTick && stationId != null && !stationId.equals(previous)) {
                onArrival(server, data, train, station, now, cfg);
            }

            boolean derailed = train.derailed;
            if (!firstTick && derailed && !LAST_DERAILED.getOrDefault(train.id, false)) {
                penalise(server, data, train, cfg, "crash");
                data.removeManifest(train.id);
            }
            LAST_DERAILED.put(train.id, derailed);

            if (firstTick || now % 20L == 0L) {
                cachePosition(train);
                syncSeatMarkers(server, data, train);
                List<TrainManifest.SeatKey> scanned = findSeats(train);
                if (!scanned.isEmpty()) {
                    TrainManifest cached = data.manifest(train.id);
                    // Only written once per train (assembly); the layout cannot change without a rebuild.
                    if (cached.layout().isEmpty()) {
                        cached.layout().clear();
                        cached.layout().addAll(scanned);
                        data.setDirty();
                    }
                }
            }
        }

        for (Iterator<UUID> iterator = LAST_STATION.keySet().iterator(); iterator.hasNext(); ) {
            UUID id = iterator.next();
            if (seen.contains(id)) {
                continue;
            }
            TrainManifest manifest = data.peekManifest(id);
            if (!firstTick && manifest != null && !manifest.isEmpty()) {
                penaliseById(server, data, id, cfg, "dismantle");
                data.removeManifest(id);
            }
            iterator.remove();
            LAST_DERAILED.remove(id);
            LAST_POS.remove(id);
            LAST_DIM.remove(id);
        }
    }

    private static GlobalStation safeCurrentStation(Train train) {
        try {
            return train.getCurrentStation();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void cachePosition(Train train) {
        for (Carriage carriage : train.carriages) {
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity != null) {
                LAST_POS.put(train.id, entity.position());
                LAST_DIM.put(train.id, entity.level().dimension());
                return;
            }
        }
    }

    // ------------------------------------------------------------------ arrival

    private static void onArrival(MinecraftServer server, VillageTransitData data, Train train,
                                  GlobalStation station, long now, VTConfig.Snapshot cfg) {
        LampRecord lamp = data.findLampForStation(station.id);
        if (lamp == null || !lamp.hasVillage()) {
            return;
        }
        StationRecord village = data.get(lamp.village);
        if (village == null || village.state != VillageStationState.ACTIVE) {
            return;
        }
        ServerLevel level = server.getLevel(lamp.village.dimension());
        if (level == null) {
            return;
        }
        TrainManifest manifest = data.manifest(train.id);
        int waitingBefore = village.passengers.size();
        int freeSeatsBefore = 0;

        List<TrainManifest.SeatKey> seats = findSeats(train);
        Set<TrainManifest.SeatKey> playerSeats = findPlayerSeats(train);
        for (TrainManifest.SeatKey seat : seats) {
            if (!manifest.seats().containsKey(seat) && !playerSeats.contains(seat)) {
                freeSeatsBefore++;
            }
        }

        // Seats that disappeared (contraption rebuilt) must not keep travellers forever.
        if (!seats.isEmpty()) {
            Set<TrainManifest.SeatKey> valid = new HashSet<>(seats);
            manifest.seats().keySet().removeIf(key -> !valid.contains(key));
        }

        ArrivalPipeline.Outcome outcome = ArrivalPipeline.apply(data, village, lamp.village, manifest,
                seats, playerSeats, train.owner, now, cfg);

        if (cfg.logTransfers) {
            CreateVillageTransit.LOGGER.info("[transfer] train {} at {} -> alighted {} stayed {} boarded {} paid {}",
                    train.id, lamp.village.pos(), outcome.alighted(), outcome.stayed(), outcome.boarded(), outcome.paid());
        }
    }

    private static List<TrainManifest.SeatKey> findSeats(Train train) {
        List<TrainManifest.SeatKey> seats = new ArrayList<>();
        for (int carriageIndex = 0; carriageIndex < train.carriages.size(); carriageIndex++) {
            Carriage carriage = train.carriages.get(carriageIndex);
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity == null) {
                continue;
            }
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }
            for (BlockPos local : contraption.getSeats()) {
                StructureTemplate.StructureBlockInfo info = contraption.getBlocks().get(local);
                if (info != null && info.state().getBlock() instanceof PassengerSeatBlock) {
                    seats.add(new TrainManifest.SeatKey(carriageIndex, local.immutable()));
                }
            }
        }
        seats.sort(Comparator.<TrainManifest.SeatKey>comparingInt(TrainManifest.SeatKey::carriage)
                .thenComparingInt(ref -> ref.local().getY())
                .thenComparingInt(ref -> ref.local().getX())
                .thenComparingInt(ref -> ref.local().getZ()));
        return seats;
    }

    private static Set<TrainManifest.SeatKey> findPlayerSeats(Train train) {
        return findPlayerSeatOwners(train).keySet();
    }

    /** Seat -> the player sitting on it, read from Create's seat mapping. */
    private static Map<TrainManifest.SeatKey, UUID> findPlayerSeatOwners(Train train) {
        Map<TrainManifest.SeatKey, UUID> occupied = new HashMap<>();
        for (int carriageIndex = 0; carriageIndex < train.carriages.size(); carriageIndex++) {
            Carriage carriage = train.carriages.get(carriageIndex);
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity == null) {
                continue;
            }
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }
            final int carriageIndexFinal = carriageIndex;
            contraption.getSeatMapping().forEach((rider, index) -> {
                if (index == null || index < 0 || index >= contraption.getSeats().size()) {
                    return;
                }
                occupied.put(new TrainManifest.SeatKey(carriageIndexFinal, contraption.getSeats().get(index)), rider);
            });        }
        return occupied;
    }

    private static void syncSeatMarkers(MinecraftServer server, VillageTransitData data, Train train) {
        Map<TrainManifest.SeatKey, UUID> owners = findPlayerSeatOwners(train);
        TrainManifest manifest = data.peekManifest(train.id);
        if (manifest == null) {
            if (owners.isEmpty()) {
                return;
            }
            manifest = data.manifest(train.id);
        }
        if (!owners.isEmpty() || SeatMarkers.hasMarkers(manifest)) {
            SeatMarkers.sync(manifest, owners, server);
            data.setDirty();
        }
        // Visual passengers are reconciled whenever the carriage is loaded, markers or not.
        SeatNpcs.sync(train, manifest);
    }

    // ------------------------------------------------------------------ penalties

    private static void penalise(MinecraftServer server, VillageTransitData data, Train train,
                                 VTConfig.Snapshot cfg, String reason) {
        penaliseById(server, data, train.id, cfg, reason);
    }

    private static void penaliseById(MinecraftServer server, VillageTransitData data, UUID trainId,
                                     VTConfig.Snapshot cfg, String reason) {
        TrainManifest manifest = data.peekManifest(trainId);
        if (manifest == null || manifest.isEmpty()) {
            return;
        }
        Vec3 pos = LAST_POS.get(trainId);
        ResourceKey<Level> dimension = LAST_DIM.get(trainId);
        // Without a known position fall back to "no distance", never to the world origin.
        GlobalPos accident = pos == null ? null
                : GlobalPos.of(dimension == null ? Level.OVERWORLD : dimension, BlockPos.containing(pos));
        long total = AccidentPenalty.total(manifest, accident, cfg);

        UUID owner = null;
        Train train = Create.RAILWAYS.trains.get(trainId);
        if (train != null) {
            owner = train.owner;
        }
        if (owner != null && total > 0) {
            data.addBalance(owner, -total);
            CreateVillageTransit.LOGGER.info("[penalty:{}] train {} owner {} -{}", reason, trainId, owner, total);
        }
    }
}
