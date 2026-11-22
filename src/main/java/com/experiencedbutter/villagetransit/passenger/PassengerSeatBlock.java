package com.experiencedbutter.villagetransit.passenger;

import com.experiencedbutter.villagetransit.registry.ModBlocks;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Create seat that can also carry one traveller while it is assembled onto a contraption.
 *
 * <p>The world behaviour is exactly Create's seat. Dyeing stays inside the passenger seat family so
 * the block identity (and the traveller it may carry) is never lost.</p>
 */
public class PassengerSeatBlock extends SeatBlock {

    public PassengerSeatBlock(Properties properties, DyeColor color) {
        super(properties, color);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        DyeColor color = DyeColor.getColor(stack);
        if (color != null && color != this.getColor()) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            BlockState newState = ModBlocks.getPassengerSeat(color).get().defaultBlockState()
                    .setValue(ProperWaterloggedBlock.WATERLOGGED, state.getValue(ProperWaterloggedBlock.WATERLOGGED));
            level.setBlockAndUpdate(pos, newState);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
