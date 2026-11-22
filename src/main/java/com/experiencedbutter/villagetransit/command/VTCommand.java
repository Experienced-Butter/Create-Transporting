package com.experiencedbutter.villagetransit.command;

import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.economy.TradeStationBlockEntity;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlocks;
import com.experiencedbutter.villagetransit.service.VillageTransitService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Small admin command used for diagnostics and for the automated smoke test.
 */
@EventBusSubscriber
public final class VTCommand {

    private VTCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cvtransit")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("selftest").executes(context -> selfTest(context.getSource())))
                .then(Commands.literal("simulate").executes(context -> simulate(context.getSource())))
                .then(Commands.literal("econ").executes(context -> econ(context.getSource())))
                .then(Commands.literal("lamp").executes(context -> lamp(context.getSource())))
                .then(Commands.literal("board").executes(context -> board(context.getSource())))
                .then(Commands.literal("accident").executes(context -> accident(context.getSource())))
                .then(Commands.literal("chunks").executes(context -> {
                    var level = context.getSource().getLevel();
                    int loaded = level.getChunkSource().getLoadedChunksCount();
                    context.getSource().sendSuccess(() -> Component.literal("chunks: loaded=" + loaded), false);
                    return loaded;
                }))
                .then(Commands.literal("trains").executes(context -> trains(context.getSource())))
                .then(Commands.literal("links").executes(context -> links(context.getSource())))
                .then(Commands.literal("markers").executes(context -> markers(context.getSource())))
                .then(Commands.literal("perf").executes(context -> {
                    String message = "perf: stations=" + VillageTransitService.lastStationCount()
                            + " tickMicros=" + (VillageTransitService.lastTickNanos() / 1000);
                    context.getSource().sendSuccess(() -> Component.literal(message), false);
                    return 1;
                }))
                .then(Commands.literal("packet").executes(context -> packet(context.getSource(), 0))
                        .then(Commands.argument("travellers",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 64))
                                .executes(context -> packet(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "travellers")))))
                .then(Commands.literal("load")
                        .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 2000))
                                .executes(context -> load(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"), 0))
                                .then(Commands.argument("travellers",
                                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 64))
                                        .executes(context -> load(context.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "travellers"))))))
                .then(Commands.literal("persist")
                        .then(Commands.literal("write").executes(context -> persistWrite(context.getSource())))
                        .then(Commands.literal("check").executes(context -> persistCheck(context.getSource()))))
                .then(Commands.literal("refresh").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    BlockPos pos = BlockPos.containing(source.getPosition());
                    VillageTransitData data = VillageTransitData.get(source.getServer());
                    StationRecord record = data.get(GlobalPos.of(source.getLevel().dimension(), pos));
                    if (record == null) {
                        source.sendFailure(Component.literal("No village station at your position"));
                        return 0;
                    }
                    VillageTransitService.requestRefresh(source.getServer(),
                            GlobalPos.of(source.getLevel().dimension(), pos), record);
                    source.sendSuccess(() -> Component.literal("Census requested"), false);
                    return 1;
                })));
    }

    /**
     * Drives the real arrival settlement with a synthetic manifest: three travellers from a village
     * 512 blocks away into a 30 villager / 20 employed village.
     */
    private static int simulate(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        BlockPos villagePos = BlockPos.containing(source.getPosition());
        GlobalPos villageKey = GlobalPos.of(level.dimension(), villagePos);

        StationRecord village = new StationRecord(villagePos);
        village.villagers = 30;
        village.employed = 20;
        village.state = com.experiencedbutter.villagetransit.data.VillageStationState.ACTIVE;

        long now = level.getGameTime();
        GlobalPos origin = GlobalPos.of(level.dimension(), villagePos.offset(512, 0, 0));
        var manifest = new com.experiencedbutter.villagetransit.data.TrainManifest();
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(0, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 3, origin, now));
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(1, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRADE, 6, origin, now));
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(2, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.RELOCATE, 1, origin, now));

        java.util.UUID owner = new java.util.UUID(0L, 51L);
        data.setBalance(owner, 0L);
        for (int i = 0; i < 2; i++) {
            village.passengers.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                    com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 5, origin, now));
        }
        java.util.List<com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey> seats = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            seats.add(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(i, 0, 0)));
        }
        java.util.Set<com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey> playerSeats =
                java.util.Set.of(seats.get(1));

        var outcome = com.experiencedbutter.villagetransit.train.ArrivalPipeline.apply(
                data, village, villageKey, manifest, seats, playerSeats, owner, now,
                com.experiencedbutter.villagetransit.config.VTConfig.get());

        String message = "simulate: paid=" + outcome.paid()
                + " credited=" + outcome.credited()
                + " balance=" + data.balance(owner)
                + " alighted=" + outcome.alighted()
                + " stayed=" + outcome.stayed()
                + " boarded=" + outcome.boarded()
                + " waiting=" + village.passengers.size();
        source.sendSuccess(() -> Component.literal(message), false);
        return outcome.alighted();
    }

    /**
     * Drives a real trade station block entity through the money invariants: minting deducts
     * before it creates a coin, deposits credit the wallet, and debt blocks withdrawals.
     */
    private static int econ(CommandSourceStack source) {
        var level = source.getLevel();
        BlockPos base = BlockPos.containing(source.getPosition());
        base = new BlockPos(base.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()), base.getZ());
        level.setBlockAndUpdate(base, ModBlocks.TRADE_STATION.get().defaultBlockState());
        if (!(level.getBlockEntity(base) instanceof com.experiencedbutter.villagetransit.economy.TradeStationBlockEntity station)) {
            source.sendFailure(Component.literal("could not place a trade station"));
            return 0;
        }
        var data = VillageTransitData.get(source.getServer());
        java.util.UUID testId = new java.util.UUID(0L, 42L);
        station.owner = testId;
        data.setBalance(testId, 1000L);

        boolean minted = station.mintOne(data, 0);
        long afterMint = data.balance(testId);
        int inSlot = station.inventory.getStackInSlot(0).getCount();

        station.inventory.setStackInSlot(0, new ItemStack(
                com.experiencedbutter.villagetransit.economy.Coins.COPPER.get(), 2));
        station.deposit(data);
        long afterDeposit = data.balance(testId);
        boolean slotEmpty = station.inventory.getStackInSlot(0).isEmpty();

        data.setBalance(testId, -50L);
        boolean mintWhileInDebt = station.mintOne(data, 0);

        data.setBalance(testId, 0L);
        level.removeBlock(base, false);

        String message = "econ: minted=" + minted
                + " balanceAfterMint=" + afterMint
                + " slot=" + inSlot
                + " balanceAfterDeposit=" + afterDeposit
                + " slotEmpty=" + slotEmpty
                + " mintWhileInDebt=" + mintWhileInDebt;
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /**
     * Exercises every lamp binding branch: success, wrong type, duplicate village, out of range and
     * unloaded target. Uses the real {@link com.experiencedbutter.villagetransit.village.LampBinding}.
     */
    private static int lamp(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        BlockPos base = BlockPos.containing(source.getPosition());
        base = new BlockPos(base.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()), base.getZ());

        BlockPos stationPos = base.east(2);
        BlockPos farStationPos = base.east(64);
        BlockPos lampA = base;
        BlockPos lampB = base.south(2);
        BlockPos plainBlock = base.west(2);

        level.setBlockAndUpdate(stationPos, ModBlocks.VILLAGE_STATION.get().defaultBlockState());
        level.setBlockAndUpdate(farStationPos, ModBlocks.VILLAGE_STATION.get().defaultBlockState());
        level.setBlockAndUpdate(lampA, ModBlocks.PLATFORM_LAMP.get().defaultBlockState());
        level.setBlockAndUpdate(lampB, ModBlocks.PLATFORM_LAMP.get().defaultBlockState());
        level.setBlockAndUpdate(plainBlock, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        data.getOrCreate(level.dimension(), stationPos);
        data.getOrCreate(level.dimension(), farStationPos);
        data.getOrCreateLamp(level.dimension(), lampA);
        data.getOrCreateLamp(level.dimension(), lampB);

        var ok = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampA, 0, stationPos);
        var duplicate = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampB, 0, stationPos);
        var wrongType = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampB, 0, plainBlock);
        var tooFar = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampB, 0, farStationPos);
        var wrongSlot = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampB, 1, stationPos);
        var unloaded = com.experiencedbutter.villagetransit.village.LampBinding.bind(level, lampB, 0,
                base.offset(20000, 0, 20000));

        var lampRecord = data.getLamp(GlobalPos.of(level.dimension(), lampB));
        lampRecord.village = null;
        lampRecord.trainStationPos = null;
        lampRecord.trainStationId = null;
        int codeUnlinked = com.experiencedbutter.villagetransit.village.LampBinding.status(level, data, lampRecord);
        lampRecord.village = GlobalPos.of(level.dimension(), stationPos);
        int codeNoStation = com.experiencedbutter.villagetransit.village.LampBinding.status(level, data, lampRecord);
        StationRecord tempStation = data.get(GlobalPos.of(level.dimension(), stationPos));
        if (tempStation != null) {
            tempStation.state = com.experiencedbutter.villagetransit.data.VillageStationState.ACTIVE;
        }
        lampRecord.trainStationPos = GlobalPos.of(level.dimension(), plainBlock);
        lampRecord.trainStationId = java.util.UUID.randomUUID();
        int codeStationMissing = com.experiencedbutter.villagetransit.village.LampBinding.status(level, data, lampRecord);

        String message = "lamp:"
                + " bind=" + ok.success()
                + " duplicate=" + duplicate.success()
                + " wrongType=" + wrongType.success()
                + " tooFar=" + tooFar.success()
                + " wrongSlot=" + wrongSlot.success()
                + " unloaded=" + unloaded.success()
                + " codes=" + codeUnlinked + "," + codeNoStation + "," + codeStationMissing
                + " reasons=" + duplicate.messageKey().replace("message.create_village_transit.", "")
                + "," + wrongType.messageKey().replace("message.create_village_transit.", "")
                + "," + tooFar.messageKey().replace("message.create_village_transit.", "")
                + "," + wrongSlot.messageKey().replace("message.create_village_transit.", "")
                + "," + unloaded.messageKey().replace("message.create_village_transit.", "");

        level.removeBlock(lampA, false);
        level.removeBlock(lampB, false);
        level.removeBlock(stationPos, false);
        level.removeBlock(farStationPos, false);
        level.removeBlock(plainBlock, false);
        data.removeLamp(GlobalPos.of(level.dimension(), lampA));
        data.removeLamp(GlobalPos.of(level.dimension(), lampB));
        data.remove(GlobalPos.of(level.dimension(), stationPos));
        data.remove(GlobalPos.of(level.dimension(), farStationPos));

        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Boarding rule: seat order, skip player seats, stop when the village list is empty. */
    private static int board(CommandSourceStack source) {
        var level = source.getLevel();
        BlockPos base = BlockPos.containing(source.getPosition());
        GlobalPos origin = GlobalPos.of(level.dimension(), base.offset(512, 0, 0));
        long now = level.getGameTime();

        StationRecord village = new StationRecord(base);
        village.villagers = 30;
        village.employed = 20;
        for (int i = 0; i < 5; i++) {
            village.passengers.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                    com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 4, origin, now));
        }

        var manifest = new com.experiencedbutter.villagetransit.data.TrainManifest();
        java.util.List<com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey> seats = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            seats.add(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(i, 0, 0)));
        }
        java.util.Set<com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey> playerSeats =
                java.util.Set.of(seats.get(1));

        int boarded = com.experiencedbutter.villagetransit.train.SeatBoarding.board(manifest, seats, playerSeats, village);
        String message = "board: boarded=" + boarded
                + " onboard=" + manifest.size()
                + " waiting=" + village.passengers.size()
                + " seat1Free=" + !manifest.seats().containsKey(seats.get(1));
        source.sendSuccess(() -> Component.literal(message), false);
        return boarded;
    }

    /** Accident rule: every onboard traveller is charged as if alighting at the accident spot. */
    private static int accident(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        BlockPos base = BlockPos.containing(source.getPosition());
        GlobalPos origin = GlobalPos.of(level.dimension(), base.offset(512, 0, 0));
        GlobalPos accident = GlobalPos.of(level.dimension(), base);
        long now = level.getGameTime();

        var manifest = new com.experiencedbutter.villagetransit.data.TrainManifest();
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(0, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRADE, 6, origin, now));
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(1, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 3, origin, now));

        long penalty = com.experiencedbutter.villagetransit.train.AccidentPenalty.total(
                manifest, accident, com.experiencedbutter.villagetransit.config.VTConfig.get());

        java.util.UUID testId = new java.util.UUID(0L, 43L);
        data.setBalance(testId, 100L);
        data.addBalance(testId, -penalty);
        long balance = data.balance(testId);

        String message = "accident: travellers=" + manifest.size()
                + " penalty=" + penalty
                + " balanceAfter=" + balance
                + " negative=" + (balance < 0);
        source.sendSuccess(() -> Component.literal(message), false);
        return (int) penalty;
    }

    private static final java.util.UUID PERSIST_PLAYER = new java.util.UUID(0L, 77L);
    private static final java.util.UUID PERSIST_TRAIN = new java.util.UUID(0L, 78L);

    /** Writes a station, a manifest and a wallet, so the next boot can prove persistence. */
    private static int persistWrite(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        BlockPos base = BlockPos.containing(source.getPosition());
        base = new BlockPos(base.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()), base.getZ());

        BlockPos stationPos = base.east(3);
        level.setBlockAndUpdate(stationPos, ModBlocks.VILLAGE_STATION.get().defaultBlockState());
        StationRecord record = data.getOrCreate(level.dimension(), stationPos);
        record.villagers = 21;
        record.employed = 13;
        record.beds = 7;
        record.workstations = 4;
        record.state = com.experiencedbutter.villagetransit.data.VillageStationState.ACTIVE;
        record.passengers.clear();
        GlobalPos origin = GlobalPos.of(level.dimension(), base.offset(128, 0, 0));
        for (int i = 0; i < 3; i++) {
            record.passengers.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                    com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRADE, 5, origin, 0L));
        }

        var manifest = data.manifest(PERSIST_TRAIN);
        manifest.seats().clear();
        manifest.seats().put(new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(0, 0, 0)),
                com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 4, origin, 0L));

        data.setBalance(PERSIST_PLAYER, 4321L);
        data.setDirty();

        String message = "persist.write: station=" + stationPos.toShortString()
                + " travellers=" + record.passengers.size()
                + " manifest=" + manifest.size()
                + " balance=" + data.balance(PERSIST_PLAYER);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int persistCheck(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        BlockPos base = BlockPos.containing(source.getPosition());
        base = new BlockPos(base.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()), base.getZ());
        StationRecord record = data.get(GlobalPos.of(level.dimension(), base.east(3)));
        var manifest = data.peekManifest(PERSIST_TRAIN);

        int fixture = 0;
        if (record != null) {
            for (ItemStack stack : record.passengers) {
                var passenger = com.experiencedbutter.villagetransit.passenger.Travellers.data(stack);
                if (passenger != null && passenger.bornTick() == 0L) {
                    fixture++;
                }
            }
        }

        String message = "persist.check: stationFound=" + (record != null)
                + " villagers=" + (record == null ? -1 : record.villagers)
                + " fixtureTravellers=" + fixture
                + " totalTravellers=" + (record == null ? -1 : record.passengers.size())
                + " manifest=" + (manifest == null ? -1 : manifest.size())
                + " balance=" + data.balance(PERSIST_PLAYER);
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** What the tracker currently sees: used while testing with a real train. */
    private static int trains(CommandSourceStack source) {
        var data = VillageTransitData.get(source.getServer());
        var trains = com.simibubi.create.Create.RAILWAYS.trains;
        StringBuilder builder = new StringBuilder();
        for (var train : trains.values()) {
            var station = train.getCurrentStation();
            var manifest = data.peekManifest(train.id);
            builder.append("\n  ").append(train.id.toString(), 0, 8)
                    .append(" station=").append(station == null ? "-" : station.id.toString().substring(0, 8))
                    .append(" derailed=").append(train.derailed)
                    .append(" carriages=").append(train.carriages.size())
                    .append(" onboard=").append(manifest == null ? 0 : manifest.size());
        }
        String message = "trains: " + trains.size() + builder;
        source.sendSuccess(() -> Component.literal(message), false);
        return trains.size();
    }

    /** Lamp wiring overview: which village/station each lamp points at and whether it is lit. */
    private static int links(CommandSourceStack source) {
        var level = source.getLevel();
        var data = VillageTransitData.get(source.getServer());
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (var entry : data.lampEntries()) {
            var lamp = entry.getValue();
            count++;
            boolean lit = com.experiencedbutter.villagetransit.village.PlatformLampBlockEntity.isLit(level, data, lamp);
            builder.append("\n  ").append(entry.getKey().pos().toShortString())
                    .append(" village=").append(lamp.village == null ? "-" : lamp.village.pos().toShortString())
                    .append(" station=").append(lamp.trainStationPos == null ? "-" : lamp.trainStationPos.pos().toShortString())
                    .append(" stationId=").append(lamp.trainStationId == null ? "-" : lamp.trainStationId.toString().substring(0, 8))
                    .append(" status=").append(com.experiencedbutter.villagetransit.village.LampBinding.status(level, data, lamp))
                    .append(" lit=").append(lit);
        }
        String message = "lamps: " + count + builder;
        source.sendSuccess(() -> Component.literal(message), false);
        return count;
    }

    /** Player-seat marker lifecycle: added while sitting, removed when getting up, travellers kept. */
    private static int markers(CommandSourceStack source) {
        var server = source.getServer();
        var level = source.getLevel();
        var manifest = new com.experiencedbutter.villagetransit.data.TrainManifest();
        var seat0 = new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(0, 0, 0));
        var seat1 = new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(1, 0, 0));
        var seat2 = new com.experiencedbutter.villagetransit.data.TrainManifest.SeatKey(0, new BlockPos(2, 0, 0));
        java.util.UUID playerId = new java.util.UUID(0L, 91L);

        com.experiencedbutter.villagetransit.train.SeatMarkers.sync(manifest,
                java.util.Map.of(seat0, playerId, seat1, playerId), server);
        int afterSit = countMarkers(manifest);

        com.experiencedbutter.villagetransit.train.SeatMarkers.sync(manifest,
                java.util.Map.of(seat1, playerId), server);
        int afterStand = countMarkers(manifest);

        manifest.seats().put(seat2, com.experiencedbutter.villagetransit.passenger.Travellers.create(
                com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 4,
                GlobalPos.of(level.dimension(), BlockPos.ZERO), 0L));
        com.experiencedbutter.villagetransit.train.SeatMarkers.sync(manifest,
                java.util.Map.of(seat1, playerId), server);
        boolean travellerKept = manifest.seats().containsKey(seat2);

        String message = "markers: afterSit=" + afterSit
                + " afterStand=" + afterStand
                + " markersNow=" + countMarkers(manifest)
                + " travellerKept=" + travellerKept;
        source.sendSuccess(() -> Component.literal(message), false);
        return afterSit;
    }

    private static int countMarkers(com.experiencedbutter.villagetransit.data.TrainManifest manifest) {
        int count = 0;
        for (ItemStack stack : manifest.seats().values()) {
            if (com.experiencedbutter.villagetransit.passenger.Travellers.isMarker(stack)) {
                count++;
            }
        }
        return count;
    }

    /** Creates or removes synthetic station records for a scheduler load test. */
    private static int load(CommandSourceStack source, int count) {
        return load(source, count, 0);
    }

    private static int load(CommandSourceStack source, int count, int travellersPerStation) {
        var data = VillageTransitData.get(source.getServer());
        var level = source.getLevel();
        java.util.List<GlobalPos> stale = new java.util.ArrayList<>();
        for (var entry : data.entries()) {
            if ("loadtest".equals(entry.getValue().villageId)) {
                stale.add(entry.getKey());
            }
        }
        stale.forEach(data::remove);

        long now = level.getGameTime();
        for (int i = 0; i < count; i++) {
            StationRecord record = data.getOrCreate(level.dimension(), new BlockPos(30000 + i * 32, 100, 30000));
            record.villageId = "loadtest";
            record.state = com.experiencedbutter.villagetransit.data.VillageStationState.UNBOUND;
            record.nextUpdateTick = now;
            record.nextCensusTick = Long.MAX_VALUE;
            record.passengers.clear();
            var origin = GlobalPos.of(level.dimension(), record.pos.offset(64, 0, 0));
            for (int t = 0; t < travellersPerStation; t++) {
                record.passengers.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                        com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRAVEL, 4, origin, now));
            }
        }
        data.setDirty();
        String message = "load: synthetic stations=" + count + " travellersEach=" + travellersPerStation;
        source.sendSuccess(() -> Component.literal(message), false);
        return count;
    }

    /** Encodes a synthetic station snapshot and reports its wire size. */
    private static int packet(CommandSourceStack source, int travellers) {
        var level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        GlobalPos origin = GlobalPos.of(level.dimension(), pos.offset(512, 0, 0));
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < travellers; i++) {
            stacks.add(com.experiencedbutter.villagetransit.passenger.Travellers.create(
                    com.experiencedbutter.villagetransit.passenger.PassengerPurpose.TRADE, 5, origin,
                    level.getGameTime()));
        }
        var payload = new com.experiencedbutter.villagetransit.network.StationDataPayload(
                pos, 1, 27, 18, 6, 12, 42, 24, false, 64, "not_in_village_structure", 1234L, 6000, stacks);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        com.experiencedbutter.villagetransit.network.StationDataPayload.STREAM_CODEC.encode(buffer, payload);
        int bytes = buffer.readableBytes();

        var lamp = new com.experiencedbutter.villagetransit.network.LampDataPayload(
                pos, true, pos.east(2), true, pos.east(4), true,
                com.experiencedbutter.villagetransit.network.LampDataPayload.STATUS_COMPLETE);
        var lampBuffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        com.experiencedbutter.villagetransit.network.LampDataPayload.STREAM_CODEC.encode(lampBuffer, lamp);

        String message = "packet: travellers=" + travellers
                + " stationBytes=" + bytes
                + " perTraveller=" + (travellers == 0 ? 0 : (bytes - 60) / travellers)
                + " lampBytes=" + lampBuffer.readableBytes();
        source.sendSuccess(() -> Component.literal(message), false);
        buffer.release();
        lampBuffer.release();
        return bytes;
    }

    private static int status(CommandSourceStack source) {
        VillageTransitData data = VillageTransitData.get(source.getServer());
        int count = 0;
        StringBuilder builder = new StringBuilder();
        for (var entry : data.entries()) {
            StationRecord record = entry.getValue();
            count++;
            builder.append("\n").append(entry.getKey().pos())
                    .append(" state=").append(record.state)
                    .append(" villagers=").append(record.villagers)
                    .append(" employed=").append(record.employed)
                    .append(" beds=").append(record.beds)
                    .append(" workstations=").append(record.workstations)
                    .append(" travellers=").append(record.passengers.size())
                    .append(" villageId=").append(record.villageId)
                    .append(" reason=").append(record.lastReason);
        }
        String message = "village stations: " + count + builder;
        source.sendSuccess(() -> Component.literal(message), false);
        return count;
    }

    /**
     * Places a station surrounded by beds and villagers at the command position, so a census can
     * actually turn it active without hunting for a generated village.
     */
    private static int selfTest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos base = BlockPos.containing(source.getPosition());
        base = new BlockPos(base.getX(), level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()), base.getZ());
        level.setBlockAndUpdate(base, ModBlocks.VILLAGE_STATION.get().defaultBlockState());
        for (int i = 0; i < 3; i++) {
            BlockPos bedPos = base.offset(2 + i, 0, 0);
            level.setBlockAndUpdate(bedPos, Blocks.RED_BED.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        }
        for (int i = 0; i < 4; i++) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager != null) {
                villager.moveTo(base.getX() + 1.5 + i, base.getY(), base.getZ() + 2.5, 0, 0);
                level.addFreshEntity(villager);
            }
        }
        GlobalPos key = GlobalPos.of(level.dimension(), base);
        StationRecord record = VillageTransitData.get(source.getServer()).getOrCreate(level.dimension(), base);
        record.nextCensusTick = level.getGameTime();
        record.nextUpdateTick = level.getGameTime() + 40L;
        final BlockPos anchor = base;
        source.sendSuccess(() -> Component.literal("Self test placed at " + anchor.toShortString())
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    @SuppressWarnings("unused")
    private static ItemStack seatSample() {
        return new ItemStack(ModBlocks.getPassengerSeat(DyeColor.WHITE).get());
    }
}
