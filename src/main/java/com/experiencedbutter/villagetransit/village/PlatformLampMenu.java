package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.registry.ModBlocks;
import com.experiencedbutter.villagetransit.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PlatformLampMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public PlatformLampMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.PLATFORM_LAMP.get(), containerId);
        this.pos = pos;
    }

    public static PlatformLampMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new PlatformLampMenu(containerId, inventory, buffer.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0 * 64.0) {
            return false;
        }
        return player.level().getBlockState(pos).is(ModBlocks.PLATFORM_LAMP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
