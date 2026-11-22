package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.economy.Coins;
import com.experiencedbutter.villagetransit.economy.TradeStationMenu;
import com.experiencedbutter.villagetransit.network.LampDataPayload;
import com.experiencedbutter.villagetransit.network.StationDataPayload;
import com.experiencedbutter.villagetransit.passenger.PassengerPurpose;
import com.experiencedbutter.villagetransit.passenger.SeatInspectMenu;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import com.experiencedbutter.villagetransit.village.PlatformLampMenu;
import com.experiencedbutter.villagetransit.village.VillageStationMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Development only: when the JVM runs with {@code -Dcvtransit.guiDemo=true}, cycles through every
 * mod screen with synthetic data so the layout can be screenshot without any input automation.
 * Does nothing in normal play.
 */
@EventBusSubscriber(modid = CreateVillageTransit.MOD_ID, value = Dist.CLIENT)
public final class GuiDemo {

    private static boolean enabled = System.getProperty("cvtransit.guiDemo") != null;
    private static int ticks;
    private static int stage;

    private GuiDemo() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!enabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ticks++;
        if (ticks < 60 || ticks % 100 != 0) {
            return;
        }
        show(minecraft, stage);
        stage = (stage + 1) % 5;
    }

    private static void show(Minecraft minecraft, int index) {
        BlockPos pos = minecraft.player.blockPosition();
        GlobalPos origin = GlobalPos.of(minecraft.level.dimension(), pos.offset(120, 0, 0));
        var inventory = minecraft.player.getInventory();

        switch (index) {
            case 0 -> {
                List<ItemStack> travellers = List.of(
                        Travellers.create(PassengerPurpose.TRAVEL, 5, origin, 0L),
                        Travellers.create(PassengerPurpose.TRADE, 8, origin, 0L),
                        Travellers.create(PassengerPurpose.RELOCATE, 10, origin, 0L));
                ClientStationCache.put(new StationDataPayload(pos, 1, 27, 18, 6, 12, 42, 24, false, 64, "", 1234L, 6000, travellers));
                minecraft.setScreen(new VillageStationScreen(
                        new VillageStationMenu(0, inventory, pos), inventory,
                        Component.translatable("block.create_village_transit.village_station")));
            }
            case 1 -> {
                ClientLampCache.put(new LampDataPayload(pos, true, pos.east(2), true, pos.east(4), true,
                        LampDataPayload.STATUS_COMPLETE));
                minecraft.setScreen(new PlatformLampScreen(
                        new PlatformLampMenu(0, inventory, pos), inventory,
                        Component.translatable("block.create_village_transit.platform_lamp")));
            }
            case 2 -> {
                ItemStackHandler handler = new ItemStackHandler(7);
                handler.setStackInSlot(0, new ItemStack(Coins.COPPER.get(), 12));
                handler.setStackInSlot(2, new ItemStack(Coins.GOLD.get(), 3));
                minecraft.setScreen(new TradeStationScreen(
                        new TradeStationMenu(0, inventory, pos, handler), inventory,
                        Component.translatable("block.create_village_transit.trade_station")));
            }
            case 3 -> {
                ItemStack traveller = Travellers.create(PassengerPurpose.TRADE, 7, origin, 0L);
                minecraft.setScreen(new SeatInspectScreen(
                        new SeatInspectMenu(0, inventory, traveller), inventory,
                        Component.translatable("screen.create_village_transit.seat")));
            }
            default -> minecraft.setScreen(null);
        }
    }
}
