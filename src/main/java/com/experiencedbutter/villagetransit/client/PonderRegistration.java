package com.experiencedbutter.villagetransit.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers the ponder plugin once the client is set up. */
@EventBusSubscriber(modid = "create_village_transit", value = Dist.CLIENT)
public final class PonderRegistration {

    private PonderRegistration() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Ponder scenes are parked: the standalone scenes render empty without a schematic file.
        // Re-enable once schematic-based scenes exist.
        // event.enqueueWork(VillageTransitPonderPlugin::register);
    }
}