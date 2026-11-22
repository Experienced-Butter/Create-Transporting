package com.experiencedbutter.villagetransit;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.registry.ModBlockEntities;
import com.experiencedbutter.villagetransit.registry.ModBlocks;
import com.experiencedbutter.villagetransit.registry.ModDataComponents;
import com.experiencedbutter.villagetransit.registry.ModItems;
import com.experiencedbutter.villagetransit.registry.ModMenus;
import com.experiencedbutter.villagetransit.service.VillageTransitService;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Entry point of Create: Village Transit.
 *
 * <p>Registration is split into small registry classes so that community pull requests can
 * touch one concern at a time. Nothing here modifies Create itself; we only read its public
 * state (railway manager, stations, contraptions).</p>
 */
@Mod(CreateVillageTransit.MOD_ID)
public class CreateVillageTransit {

    public static final String MOD_ID = "create_village_transit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateVillageTransit(IEventBus modBus, ModContainer container) {
        VTConfig.register(container);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModDataComponents.DATA_COMPONENTS.register(modBus);
        ModMenus.MENUS.register(modBus);
        com.experiencedbutter.villagetransit.economy.Coins.COIN_ITEMS.register(modBus);
        com.experiencedbutter.villagetransit.registry.ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(com.experiencedbutter.villagetransit.network.ModNetwork::register);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        ModBlockEntities.TRADE_STATION.get(),
                        (be, side) -> be.inventory));

        modBus.addListener((ModConfigEvent.Loading event) -> VTConfig.bake());
        modBus.addListener((ModConfigEvent.Reloading event) -> VTConfig.bake());

        NeoForge.EVENT_BUS.addListener(VillageTransitService::onServerTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) ->
                com.experiencedbutter.villagetransit.train.TrainTracker.reset());

        LOGGER.info("[{}] initialised", MOD_ID);
    }

    /**
     * Compile-time canary: fails the build if the Create dependency ever disappears or is
     * resolved to an incompatible coordinate.
     */
    @SuppressWarnings("unused")
    private static void verifyCreateDependency() {
        var ignored = com.simibubi.create.AllBlocks.TRACK_STATION;
    }
}
