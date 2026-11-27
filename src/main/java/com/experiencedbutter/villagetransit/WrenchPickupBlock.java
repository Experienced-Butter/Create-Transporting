package com.experiencedbutter.villagetransit;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sneak + wrench dismantles our blocks, returning the block item while the block's own
 * {@code onRemove} still spills its contents (travellers, stored coins).
 */
public interface WrenchPickupBlock extends IWrenchable {

    @Override
    default InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && !player.mayBuild()) {
            return InteractionResult.SUCCESS;
        }
        level.destroyBlock(pos, false, player);
        ItemStack picked = new ItemStack(state.getBlock().asItem());
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Put the block straight into the player's inventory, like Create's own wrench.
            if (!serverPlayer.getInventory().add(picked)) {
                Block.popResource(level, pos, picked);
            } else {
                serverPlayer.containerMenu.broadcastChanges();
            }
        } else {
            Block.popResource(level, pos, picked);
        }
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    default InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // Plain wrench use stays out of the way so the station GUIs keep working.
        return InteractionResult.PASS;
    }
}
