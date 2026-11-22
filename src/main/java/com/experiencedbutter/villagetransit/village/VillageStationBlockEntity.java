package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Facade block entity. It owns no simulation: all state lives in {@link VillageTransitData} and is
 * driven by {@link com.experiencedbutter.villagetransit.service.VillageTransitService}.
 */
public class VillageStationBlockEntity extends BlockEntity {

    public VillageStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VILLAGE_STATION.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            VillageTransitData.get(serverLevel.getServer()).getOrCreate(serverLevel.dimension(), worldPosition);
        }
    }

    public StationRecord record() {
        if (level instanceof ServerLevel serverLevel) {
            return VillageTransitData.get(serverLevel.getServer())
                    .get(StationRecord.key(serverLevel.dimension(), worldPosition));
        }
        return null;
    }
}
