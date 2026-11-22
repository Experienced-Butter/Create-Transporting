package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.passenger.PassengerSeatBlock;
import com.experiencedbutter.villagetransit.village.PlatformLampBlock;
import com.experiencedbutter.villagetransit.village.VillageStationBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateVillageTransit.MOD_ID);

    public static final DeferredBlock<VillageStationBlock> VILLAGE_STATION = BLOCKS.register(
            "village_station",
            () -> new VillageStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(VillageStationBlock.ACTIVE) ? 7 : 0)));

    public static final DeferredBlock<PlatformLampBlock> PLATFORM_LAMP = BLOCKS.register(
            "platform_lamp",
            () -> new PlatformLampBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.5F, 4.0F)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(PlatformLampBlock.LIT) ? 15 : 0)));

    public static final DeferredBlock<com.experiencedbutter.villagetransit.economy.TradeStationBlock> TRADE_STATION =
            BLOCKS.register("trade_station",
                    () -> new com.experiencedbutter.villagetransit.economy.TradeStationBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GREEN)
                                    .strength(3.0F, 6.0F)
                                    .sound(SoundType.WOOD)));

    private static final Map<DyeColor, DeferredBlock<PassengerSeatBlock>> PASSENGER_SEATS =
            new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "passenger_seat_" + color.getName().toLowerCase(Locale.ROOT);
            PASSENGER_SEATS.put(color, BLOCKS.register(name,
                    () -> new PassengerSeatBlock(BlockBehaviour.Properties.of()
                            .mapColor(color.getMapColor())
                            .strength(0.5F)
                            .sound(SoundType.WOOL)
                            .noOcclusion(), color)));
        }
    }

    public static DeferredBlock<PassengerSeatBlock> getPassengerSeat(DyeColor color) {
        return PASSENGER_SEATS.get(color);
    }

    public static Map<DyeColor, DeferredBlock<PassengerSeatBlock>> passengerSeats() {
        return PASSENGER_SEATS;
    }

    private ModBlocks() {
    }
}
