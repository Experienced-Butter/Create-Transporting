package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.PassengerPurpose;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Visual passengers: each traveller in a seat gets an inert villager riding the same seat so
 * occupied seats are readable in the world. Villagers are tagged, ignored by the village census
 * and never mistaken for a player by the seat rules.
 *
 * <p>The outfit tells the traveller's purpose: artisans for trade, gentle or scholarly professions
 * for travel, plain clothes for relocation. The clothing style follows the biome of the origin.</p>
 */
public final class SeatNpcs {

    public static final String TAG = "create_village_transit_passenger";
    private static final String SEAT = "create_village_transit_seat_v2";

    private static final VillagerProfession[] ARTISANS = {
            VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH, VillagerProfession.TOOLSMITH};
    private static final VillagerProfession[] GENTLE = {
            VillagerProfession.LIBRARIAN, VillagerProfession.CARTOGRAPHER, VillagerProfession.SHEPHERD};

    private SeatNpcs() {
    }

    public static boolean isNpc(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG);
    }

    public static void sync(Train train, TrainManifest manifest) {
        for (int carriageIndex = 0; carriageIndex < train.carriages.size(); carriageIndex++) {
            Carriage carriage = train.carriages.get(carriageIndex);
            CarriageContraptionEntity entity = carriage.anyAvailableEntity();
            if (entity == null || !(entity.level() instanceof ServerLevel level)) {
                continue;
            }
            Contraption contraption = entity.getContraption();
            if (contraption == null) {
                continue;
            }

            List<Entity> stale = new ArrayList<>();
            for (Entity passenger : entity.getPassengers()) {
                if (!isNpc(passenger)) {
                    continue;
                }
                String seat = passenger.getPersistentData().getString(SEAT);
                ItemStack stack = seatOf(manifest, carriageIndex, contraption, seat);
                if (stack == null || Travellers.isMarker(stack)) {
                    stale.add(passenger);
                }
            }
            stale.forEach(Entity::discard);

            for (Map.Entry<TrainManifest.SeatKey, ItemStack> entry : manifest.seats().entrySet()) {
                TrainManifest.SeatKey key = entry.getKey();
                if (key.carriage() != carriageIndex || Travellers.isMarker(entry.getValue())) {
                    continue;
                }
                int index = contraption.getSeats().indexOf(key.local());
                if (index < 0) {
                    continue;
                }
                String seat = carriageIndex + ":" + index;
                boolean present = entity.getPassengers().stream()
                        .anyMatch(p -> p.getPersistentData().getString(SEAT).equals(seat));
                if (!present) {
                    spawn(level, entity, index, seat, Travellers.data(entry.getValue()));
                }
            }
        }
    }

    private static ItemStack seatOf(TrainManifest manifest, int carriageIndex, Contraption contraption, String seat) {
        String[] parts = seat.split(":");
        if (parts.length != 2) {
            return null;
        }
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (index < 0 || index >= contraption.getSeats().size()) {
            return null;
        }
        BlockPos local = contraption.getSeats().get(index);
        return manifest.seats().get(new TrainManifest.SeatKey(carriageIndex, local));
    }

    private static void spawn(ServerLevel level, CarriageContraptionEntity entity, int index, String seat,
                              PassengerData data) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) {
            return;
        }
        BlockPos pos = entity.blockPosition();
        villager.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
        villager.setNoAi(true);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.getPersistentData().putBoolean(TAG, true);
        villager.getPersistentData().putString(SEAT, seat);
        dress(villager, level, data);
        level.addFreshEntity(villager);
        entity.addSittingPassenger(villager, index);
    }

    /** Outfit follows the purpose, the villager type follows the biome the traveller came from. */
    private static void dress(Villager villager, ServerLevel level, PassengerData data) {
        VillagerProfession profession = VillagerProfession.NONE;
        if (data != null) {
            PassengerPurpose purpose = data.purpose();
            if (purpose == PassengerPurpose.TRADE) {
                profession = ARTISANS[level.getRandom().nextInt(ARTISANS.length)];
            } else if (purpose == PassengerPurpose.TRAVEL) {
                profession = GENTLE[level.getRandom().nextInt(GENTLE.length)];
            }
        }
        BlockPos reference = data != null && data.origin() != null
                && level.hasChunk(data.origin().pos().getX() >> 4, data.origin().pos().getZ() >> 4)
                ? data.origin().pos() : villager.blockPosition();
        VillagerType type = VillagerType.byBiome(level.getBiome(reference));
        VillagerData villagerData = villager.getVillagerData();
        villager.setVillagerData(villagerData
                .setType(type)
                .setProfession(profession)
                .setLevel(1));
    }
}
