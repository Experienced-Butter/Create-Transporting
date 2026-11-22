package com.experiencedbutter.villagetransit.village;

import com.experiencedbutter.villagetransit.data.LampRecord;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PlatformLampBlock extends BaseEntityBlock implements com.experiencedbutter.villagetransit.WrenchPickupBlock {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    private static final VoxelShape SHAPE = Shapes.block();

    public PlatformLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PlatformLampBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlatformLampBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 2.5, 10 / 16.0);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof PlatformLampBlockEntity lamp) {
                PlatformLampBlockEntity.serverTick((ServerLevel) lvl, pos, st, lamp);
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            VillageTransitData.get(serverLevel.getServer()).getOrCreateLamp(serverLevel.dimension(), pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            LampRecord record = VillageTransitData.get(((ServerLevel) level).getServer())
                    .getLamp(GlobalPos.of(level.dimension(), pos));
            if (record != null) {
                record.village = null;
                record.trainStationPos = null;
                record.trainStationId = null;
                VillageTransitData.get(((ServerLevel) level).getServer()).setDirty();
                player.displayClientMessage(Component.translatable("message.create_village_transit.lamp_cleared"), true);
            }
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, p) -> new PlatformLampMenu(id, inventory, pos),
                        Component.translatable("block.create_village_transit.platform_lamp")),
                buffer -> buffer.writeBlockPos(pos));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.experiencedbutter.villagetransit.network.ModNetwork.sendLampData(serverPlayer, (ServerLevel) level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            VillageTransitData.get(serverLevel.getServer()).removeLamp(GlobalPos.of(serverLevel.dimension(), pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
