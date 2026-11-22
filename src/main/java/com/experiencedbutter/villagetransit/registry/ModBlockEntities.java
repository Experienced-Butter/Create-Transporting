package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.village.VillageStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateVillageTransit.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillageStationBlockEntity>> VILLAGE_STATION =
            BLOCK_ENTITIES.register("village_station", () -> BlockEntityType.Builder
                    .of(VillageStationBlockEntity::new, ModBlocks.VILLAGE_STATION.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.experiencedbutter.villagetransit.village.PlatformLampBlockEntity>> PLATFORM_LAMP =
            BLOCK_ENTITIES.register("platform_lamp", () -> BlockEntityType.Builder
                    .of(com.experiencedbutter.villagetransit.village.PlatformLampBlockEntity::new, ModBlocks.PLATFORM_LAMP.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.experiencedbutter.villagetransit.economy.TradeStationBlockEntity>> TRADE_STATION =
            BLOCK_ENTITIES.register("trade_station", () -> BlockEntityType.Builder
                    .of(com.experiencedbutter.villagetransit.economy.TradeStationBlockEntity::new, ModBlocks.TRADE_STATION.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
