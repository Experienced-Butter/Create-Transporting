package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.passenger.PassengerPurpose;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                    CreateVillageTransit.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tab.create_village_transit.main"))
                    .icon(() -> new ItemStack(ModItems.VILLAGE_STATION.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.VILLAGE_STATION.get());
                        output.accept(ModItems.PLATFORM_LAMP.get());
                        output.accept(ModItems.TRADE_STATION.get());
                        ModItems.passengerSeats().forEach((color, item) -> output.accept(item.get()));
                        com.experiencedbutter.villagetransit.economy.Coins.ALL
                                .forEach(coin -> output.accept(coin.get()));
                        output.accept(Travellers.create(PassengerPurpose.TRAVEL, 4,
                                GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0L));
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
