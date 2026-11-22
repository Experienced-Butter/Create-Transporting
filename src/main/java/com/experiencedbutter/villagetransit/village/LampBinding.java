package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.LampRecord;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageStationState;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server side validation for lamp links. Mirrors how Create validates package port targets:
 * type, loaded chunk and distance are all checked before anything is written.
 */
public final class LampBinding {

    public record Result(boolean success, String messageKey) {
        static Result ok() {
            return new Result(true, "");
        }

        static Result fail(String key) {
            return new Result(false, key);
        }
    }

    private LampBinding() {
    }

    public static Result bind(ServerLevel level, BlockPos lampPos, int slot, BlockPos target) {
        VTConfig.Snapshot cfg = VTConfig.get();
        if (!level.getBlockState(lampPos).is(com.experiencedbutter.villagetransit.registry.ModBlocks.PLATFORM_LAMP.get())) {
            return Result.fail("message.create_village_transit.lamp_missing");
        }
        if (!level.isLoaded(target)) {
            return Result.fail("message.create_village_transit.target_unloaded");
        }
        if (lampPos.distSqr(target) > (double) cfg.maxLinkDistance * cfg.maxLinkDistance) {
            return Result.fail("message.create_village_transit.target_too_far");
        }
        if (lampPos.equals(target)) {
            return Result.fail("message.create_village_transit.target_self");
        }

        VillageTransitData data = VillageTransitData.get(level.getServer());
        LampRecord lamp = data.getOrCreateLamp(level.dimension(), lampPos);

        if (slot == 0) {
            if (!(level.getBlockState(target).getBlock() instanceof VillageStationBlock)) {
                return Result.fail("message.create_village_transit.target_not_village");
            }
            StationRecord station = data.get(GlobalPos.of(level.dimension(), target));
            if (station == null) {
                return Result.fail("message.create_village_transit.target_not_village");
            }
            if (!cfg.allowSharedLinks && isVillageLinkedElsewhere(data, lampPos, target)) {
                return Result.fail("message.create_village_transit.village_already_linked");
            }
            lamp.village = GlobalPos.of(level.dimension(), target);
            data.setDirty();
            return Result.ok();
        }

        if (slot == 1) {
            BlockEntity be = level.getBlockEntity(target);
            if (!(be instanceof StationBlockEntity stationBlockEntity)) {
                return Result.fail("message.create_village_transit.target_not_station");
            }
            GlobalStation station = stationBlockEntity.getStation();
            if (station == null) {
                return Result.fail("message.create_village_transit.station_not_connected");
            }
            if (!cfg.allowSharedLinks && isStationLinkedElsewhere(data, lampPos, station.id)) {
                return Result.fail("message.create_village_transit.station_already_linked");
            }
            lamp.trainStationPos = GlobalPos.of(level.dimension(), target);
            lamp.trainStationId = station.id;
            data.setDirty();
            return Result.ok();
        }

        return Result.fail("message.create_village_transit.invalid_slot");
    }

    private static boolean isVillageLinkedElsewhere(VillageTransitData data, BlockPos lampPos, BlockPos villagePos) {
        for (var entry : data.lampEntries()) {
            if (entry.getKey().pos().equals(lampPos)) {
                continue;
            }
            LampRecord other = entry.getValue();
            if (other.village != null && other.village.pos().equals(villagePos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStationLinkedElsewhere(VillageTransitData data, BlockPos lampPos, java.util.UUID id) {
        for (var entry : data.lampEntries()) {
            if (entry.getKey().pos().equals(lampPos)) {
                continue;
            }
            LampRecord other = entry.getValue();
            if (id.equals(other.trainStationId)) {
                return true;
            }
        }
        return false;
    }

    public static int status(ServerLevel level, VillageTransitData data, LampRecord lamp) {
        if (!lamp.hasVillage()) {
            return com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_NO_VILLAGE;
        }
        if (!lamp.hasTrainStation()) {
            return com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_NO_STATION;
        }
        StationRecord village = data.get(lamp.village);
        VTConfig.Snapshot cfg = VTConfig.get();
        if (village == null || (cfg.lampRequiresActiveStation && village.state != VillageStationState.ACTIVE)) {
            return com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_VILLAGE_INACTIVE;
        }
        if (level.isLoaded(lamp.trainStationPos.pos())
                && !(level.getBlockEntity(lamp.trainStationPos.pos()) instanceof StationBlockEntity)) {
            return com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_STATION_MISSING;
        }
        return com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_COMPLETE;
    }

    public static void notify(ServerPlayer player, Result result) {
        if (!result.success()) {
            player.displayClientMessage(Component.translatable(result.messageKey()), true);
        }
    }
}
