package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.village.VillageStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreateVillageTransit.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<VillageStationMenu>> VILLAGE_STATION =
            MENUS.register("village_station", () -> IMenuTypeExtension.create(VillageStationMenu::create));

    public static final DeferredHolder<MenuType<?>, MenuType<com.experiencedbutter.villagetransit.village.PlatformLampMenu>> PLATFORM_LAMP =
            MENUS.register("platform_lamp", () -> IMenuTypeExtension.create(
                    com.experiencedbutter.villagetransit.village.PlatformLampMenu::create));

    public static final DeferredHolder<MenuType<?>, MenuType<com.experiencedbutter.villagetransit.economy.TradeStationMenu>> TRADE_STATION =
            MENUS.register("trade_station", () -> IMenuTypeExtension.create(
                    com.experiencedbutter.villagetransit.economy.TradeStationMenu::create));

    public static final DeferredHolder<MenuType<?>, MenuType<com.experiencedbutter.villagetransit.passenger.SeatInspectMenu>> SEAT_INSPECT =
            MENUS.register("seat_inspect", () -> IMenuTypeExtension.create(
                    com.experiencedbutter.villagetransit.passenger.SeatInspectMenu::create));

    private ModMenus() {
    }
}
