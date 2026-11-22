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

public class VillageStationMenu extends AbstractContainerMenu {

    private final BlockPos pos;

    public VillageStationMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.VILLAGE_STATION.get(), containerId);
        this.pos = pos;
    }

    public static VillageStationMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new VillageStationMenu(containerId, inventory, buffer.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        // Distance first: reading the block state of a far away position would load its chunk.
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0 * 64.0) {
            return false;
        }
        return player.level().getBlockState(pos).is(ModBlocks.VILLAGE_STATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
