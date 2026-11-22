package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.network.LampBindPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Turns the next right click on a block into a lamp link, after the player picked a slot in the
 * platform lamp screen.
 */
@EventBusSubscriber(modid = CreateVillageTransit.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!SelectionMode.isActive() || !event.getLevel().isClientSide()) {
            return;
        }
        event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
        event.setCanceled(true);
        PacketDistributor.sendToServer(new LampBindPayload(
                SelectionMode.lampPos(), SelectionMode.slot(), event.getPos()));
        SelectionMode.clear();
    }
}
