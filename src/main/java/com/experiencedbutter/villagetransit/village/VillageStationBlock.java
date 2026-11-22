package com.experiencedbutter.villagetransit.village;

import com.mojang.serialization.MapCodec;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.service.VillageTransitService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class VillageStationBlock extends BaseEntityBlock implements com.experiencedbutter.villagetransit.WrenchPickupBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public VillageStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(VillageStationBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageStationBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        com.experiencedbutter.villagetransit.config.VTConfig.Snapshot cfg =
                com.experiencedbutter.villagetransit.config.VTConfig.get();
        VillageTransitData data = VillageTransitData.get(serverLevel.getServer());

        // Enforce minimum distance between stations.
        for (var entry : data.entries()) {
            if (!entry.getKey().dimension().equals(serverLevel.dimension())) {
                continue;
            }
            double min = cfg.minStationSeparation;
            if (entry.getKey().pos().distSqr(pos) < min * min) {
                reject(serverLevel, pos, placer, "message.create_village_transit.station_too_close");
                return;
            }
        }

        // One station per village structure.
        if (cfg.oneStationPerVillage) {
            var start = serverLevel.structureManager()
                    .getStructureWithPieceAt(pos, net.minecraft.tags.StructureTags.VILLAGE);
            if (start != null && start.isValid()) {
                var chunkPos = start.getChunkPos();
                String villageId = chunkPos.x + "," + chunkPos.z;
                for (var entry : data.entries()) {
                    if (villageId.equals(entry.getValue().villageId)) {
                        reject(serverLevel, pos, placer, "message.create_village_transit.village_has_station");
                        return;
                    }
                }
            }
        }

        StationRecord record = data.getOrCreate(serverLevel.dimension(), pos);
        long now = serverLevel.getGameTime();
        record.nextCensusTick = now + 40L;
        record.nextUpdateTick = now + 200L;
    }

    private static void reject(ServerLevel level, BlockPos pos, LivingEntity placer, String messageKey) {
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(com.experiencedbutter.villagetransit.registry.ModItems.VILLAGE_STATION.get()));
        if (placer instanceof Player player) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        GlobalPos key = GlobalPos.of(serverLevel.dimension(), pos);
        StationRecord record = VillageTransitData.get(serverLevel.getServer()).get(key);
        if (record == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            VillageTransitService.requestRefresh(serverLevel.getServer(), key, record);
            player.displayClientMessage(Component.translatable("message.create_village_transit.refresh_requested"), true);
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, p) -> new VillageStationMenu(id, inventory, pos),
                        Component.translatable("block.create_village_transit.village_station")),
                buffer -> buffer.writeBlockPos(pos));
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.experiencedbutter.villagetransit.network.ModNetwork.sendData(serverPlayer, serverLevel, record, key);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            GlobalPos key = GlobalPos.of(serverLevel.dimension(), pos);
            VillageTransitData data = VillageTransitData.get(serverLevel.getServer());
            StationRecord record = data.get(key);
            if (record != null) {
                // Travellers never exist as world items; breaking the station discards them.
                data.remove(key);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
