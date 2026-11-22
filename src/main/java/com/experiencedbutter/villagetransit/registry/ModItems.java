package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.passenger.PassengerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateVillageTransit.MOD_ID);

    public static final DeferredItem<BlockItem> VILLAGE_STATION =
            ITEMS.registerSimpleBlockItem("village_station", ModBlocks.VILLAGE_STATION);

    public static final DeferredItem<BlockItem> PLATFORM_LAMP =
            ITEMS.registerSimpleBlockItem("platform_lamp", ModBlocks.PLATFORM_LAMP);

    public static final DeferredItem<BlockItem> TRADE_STATION =
            ITEMS.registerSimpleBlockItem("trade_station", ModBlocks.TRADE_STATION);

    public static final DeferredItem<PassengerItem> PASSENGER =
            ITEMS.register("passenger", () -> new PassengerItem(new PassengerItem.Properties().stacksTo(1)));

    /** Internal marker for "a player is sitting here"; never obtainable in survival. */
    public static final DeferredItem<net.minecraft.world.item.Item> SEAT_MARKER =
            ITEMS.register("seat_occupant_marker",
                    () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1)));

    private static final Map<DyeColor, DeferredItem<BlockItem>> PASSENGER_SEATS =
            new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "passenger_seat_" + color.getName().toLowerCase(Locale.ROOT);
            PASSENGER_SEATS.put(color, ITEMS.registerSimpleBlockItem(name, ModBlocks.getPassengerSeat(color)));
        }
    }

    public static Map<DyeColor, DeferredItem<BlockItem>> passengerSeats() {
        return PASSENGER_SEATS;
    }

    private ModItems() {
    }
}
