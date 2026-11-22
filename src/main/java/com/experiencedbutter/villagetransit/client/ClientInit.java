package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CreateVillageTransit.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {

    private ClientInit() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.VILLAGE_STATION.get(), VillageStationScreen::new);
        event.register(ModMenus.PLATFORM_LAMP.get(), PlatformLampScreen::new);
        event.register(ModMenus.TRADE_STATION.get(), TradeStationScreen::new);
        event.register(ModMenus.SEAT_INSPECT.get(), SeatInspectScreen::new);
    }
}
