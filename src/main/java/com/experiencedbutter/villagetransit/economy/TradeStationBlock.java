package com.experiencedbutter.villagetransit.economy;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TradeStationBlock extends BaseEntityBlock implements com.experiencedbutter.villagetransit.WrenchPickupBlock {

    /** Texture-only indicator: lit while the station receives redstone, no light level change. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("lit");

    public TradeStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(TradeStationBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TradeStationBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof TradeStationBlockEntity station) {
                TradeStationBlockEntity.serverTick((ServerLevel) lvl, pos, st, station);
                boolean powered = lvl.hasNeighborSignal(pos);
                if (st.getValue(LIT) != powered) {
                    lvl.setBlock(pos, st.setValue(LIT, powered), Block.UPDATE_CLIENTS);
                }
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof net.minecraft.server.level.ServerPlayer player
                && level.getBlockEntity(pos) instanceof TradeStationBlockEntity station) {
            station.owner = player.getUUID();
            station.setChanged();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TradeStationBlockEntity station)) {
            return InteractionResult.PASS;
        }
        if (VTConfig.get().ownerOnlyGui && station.owner != null && !station.owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.create_village_transit.trade_not_owner"), true);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, p) -> new TradeStationMenu(id, inventory, pos, station.inventory),
                        Component.translatable("block.create_village_transit.trade_station")),
                buffer -> buffer.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TradeStationBlockEntity station) {
            for (int i = 0; i < station.inventory.getSlots(); i++) {
                ItemStack stack = station.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof TradeStationBlockEntity station
                && station.owner != null) {
            VillageTransitData.get(serverLevel.getServer()).setDirty();
        }
    }

    public static boolean isTradeStation(BlockState state) {
        return state.is(ModBlocks.TRADE_STATION.get());
    }
}
