package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.LampRecord;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageStationState;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A lamp needs both links before it lights up, and the village station has to be active.
 */
public class PlatformLampBlockEntity extends BlockEntity {

    private int tickCounter;

    public PlatformLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATFORM_LAMP.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PlatformLampBlockEntity lamp) {
        if (++lamp.tickCounter < 20) {
            return;
        }
        lamp.tickCounter = 0;
        VillageTransitData data = VillageTransitData.get(level.getServer());
        LampRecord record = data.getLamp(GlobalPos.of(level.dimension(), pos));
        if (record == null) {
            return;
        }
        boolean lit = isLit(level, data, record);
        if (state.getValue(PlatformLampBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PlatformLampBlock.LIT, lit), 3);
        }
    }

    public static boolean isLit(ServerLevel level, VillageTransitData data, LampRecord record) {
        if (!record.fullyLinked()) {
            return false;
        }
        VTConfig.Snapshot cfg = VTConfig.get();
        StationRecord village = data.get(record.village);
        if (village == null) {
            return false;
        }
        if (cfg.lampRequiresActiveStation && village.state != VillageStationState.ACTIVE) {
            return false;
        }
        if (level.isLoaded(record.trainStationPos.pos())) {
            BlockEntity be = level.getBlockEntity(record.trainStationPos.pos());
            return be instanceof com.simibubi.create.content.trains.station.StationBlockEntity;
        }
        // Unloaded station blocks are trusted; the link is revalidated whenever they load.
        return true;
    }
}
