package com.experiencedbutter.villagetransit.passenger;

import com.experiencedbutter.villagetransit.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only view of the traveller sitting on a passenger seat. Opened by sneak-using the seat.
 */
public class SeatInspectMenu extends AbstractContainerMenu {

    private final ItemStack traveller;

    public SeatInspectMenu(int containerId, Inventory inventory, ItemStack traveller) {
        super(ModMenus.SEAT_INSPECT.get(), containerId);
        this.traveller = traveller.copyWithCount(1);
    }

    public static SeatInspectMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new SeatInspectMenu(containerId, inventory, ItemStack.STREAM_CODEC.decode(buffer));
    }

    public ItemStack traveller() {
        return traveller;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
