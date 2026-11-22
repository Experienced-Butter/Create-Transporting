package com.experiencedbutter.villagetransit.network;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlocks;
import com.experiencedbutter.villagetransit.service.VillageTransitService;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class ModNetwork {

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(StationDataPayload.TYPE, StationDataPayload.STREAM_CODEC, ModNetwork::handleData);
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.STREAM_CODEC, ModNetwork::handleAction);
        registrar.playToClient(LampDataPayload.TYPE, LampDataPayload.STREAM_CODEC, ModNetwork::handleLampData);
        registrar.playToServer(LampBindPayload.TYPE, LampBindPayload.STREAM_CODEC, ModNetwork::handleLampBind);
    }

    private static void handleData(StationDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.experiencedbutter.villagetransit.client.ClientStationCache.put(payload);
            }
        });
    }

    private static void handleAction(StationActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) > 4096) {
                return;
            }
            if (!level.getBlockState(payload.pos()).is(ModBlocks.VILLAGE_STATION.get())) {
                return;
            }
            GlobalPos key = GlobalPos.of(level.dimension(), payload.pos());
            StationRecord record = VillageTransitData.get(level.getServer()).get(key);
            if (record == null) {
                return;
            }
            if (payload.action() == StationActionPayload.Action.REFRESH) {
                VillageTransitService.requestRefresh(level.getServer(), key, record);
            }
            sendData(player, level, record, key);
        });
    }

    public static void sendData(ServerPlayer player, ServerLevel level, StationRecord record, GlobalPos key) {
        int seconds = (int) Math.max(0L, (record.nextCensusTick - level.getGameTime()) / 20L);
        List<ItemStack> copy = new ArrayList<>(record.passengers.size());
        for (ItemStack stack : record.passengers) {
            copy.add(stack.copy());
        }
        PacketDistributor.sendToPlayer(player, new StationDataPayload(
                key.pos(),
                record.state.ordinal(),
                record.villagers,
                record.employed,
                record.workstations,
                record.beds,
                seconds,
                (int) Math.max(0, (record.nextUpdateTick - level.getGameTime()) / 20L),
                record.refreshInFlight,
                com.experiencedbutter.villagetransit.config.VTConfig.get().passengerCapacity,
                record.lastReason == null ? "" : record.lastReason,
                record.payoutWindowSum,
                com.experiencedbutter.villagetransit.config.VTConfig.get().hourlyCapPerStation,
                copy));
        if (com.experiencedbutter.villagetransit.config.VTConfig.get().logCensus) {
            CreateVillageTransit.LOGGER.info("[sync] sent station data for {}", key.pos());
        }
    }

    private static void handleLampData(LampDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.experiencedbutter.villagetransit.client.ClientLampCache.put(payload);
                com.experiencedbutter.villagetransit.client.LampOutline.show(payload.pos(),
                        payload.hasVillage() ? payload.villagePos() : null,
                        payload.hasStation() ? payload.stationPos() : null,
                        net.minecraft.client.Minecraft.getInstance().level.dimension());
            }
        });
    }

    private static void handleLampBind(LampBindPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (player.distanceToSqr(payload.lampPos().getX() + 0.5, payload.lampPos().getY() + 0.5,
                    payload.lampPos().getZ() + 0.5) > 4096) {
                return;
            }
            var result = com.experiencedbutter.villagetransit.village.LampBinding.bind(
                    level, payload.lampPos(), payload.slot(), payload.target());
            com.experiencedbutter.villagetransit.village.LampBinding.notify(player, result);
            sendLampData(player, level, payload.lampPos());
        });
    }

    public static void sendLampData(ServerPlayer player, ServerLevel level, BlockPos lampPos) {
        var data = VillageTransitData.get(level.getServer());
        var lamp = data.getLamp(GlobalPos.of(level.dimension(), lampPos));
        if (lamp == null) {
            return;
        }
        int status = com.experiencedbutter.villagetransit.village.LampBinding.status(level, data, lamp);
        boolean lit = com.experiencedbutter.villagetransit.village.PlatformLampBlockEntity.isLit(level, data, lamp);
        PacketDistributor.sendToPlayer(player, new LampDataPayload(lampPos,
                lamp.hasVillage(), lamp.village != null ? lamp.village.pos() : null,
                lamp.hasTrainStation(), lamp.trainStationPos != null ? lamp.trainStationPos.pos() : null,
                lit, status));
    }
}
