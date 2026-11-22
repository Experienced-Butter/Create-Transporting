package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Four short ponder scenes, one per block, registered under this mod's own ponder category. */
public class VillageTransitPonderPlugin implements PonderPlugin {

    public static void register() {
        PonderIndex.addPlugin(new VillageTransitPonderPlugin());
    }

    @Override
    public String getModId() {
        return "create_village_transit";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ModBlocks.VILLAGE_STATION.getId())
                .addStoryBoard("village_station", VillageTransitPonderPlugin::villageStation);
        helper.forComponents(ModBlocks.PLATFORM_LAMP.getId())
                .addStoryBoard("platform_lamp", VillageTransitPonderPlugin::platformLamp);
        helper.forComponents(ModBlocks.TRADE_STATION.getId())
                .addStoryBoard("trade_station", VillageTransitPonderPlugin::tradeStation);
    }

    private static void villageStation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("village_station", "Village Station");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().setBlock(new net.minecraft.core.BlockPos(2, 1, 2), ModBlocks.VILLAGE_STATION.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.village_station.text_1")
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.village_station.text_2")
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void platformLamp(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("platform_lamp", "Platform Lamp");
        net.minecraft.world.level.block.Block lampBlock = ModBlocks.PLATFORM_LAMP.get();        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().setBlock(new net.minecraft.core.BlockPos(2, 1, 2), ModBlocks.PLATFORM_LAMP.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.platform_lamp.text_1")
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.platform_lamp.text_2")
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void passengerSeat(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("passenger_seat", "Passenger Seat");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().setBlock(new net.minecraft.core.BlockPos(2, 1, 2), ModBlocks.VILLAGE_STATION.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("On a train the seat carries one traveller; occupied seats cannot be used by players.")
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("Sneak and use a seat to read who is sitting there.")
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    private static void tradeStation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("trade_station", "Trade Station");
        net.minecraft.world.level.block.Block tradeBlock = ModBlocks.TRADE_STATION.get();        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().setBlock(new net.minecraft.core.BlockPos(2, 1, 2), ModBlocks.TRADE_STATION.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.NORTH);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.trade_station.text_1")
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("create_village_transit.ponder.trade_station.text_2")
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }
}
