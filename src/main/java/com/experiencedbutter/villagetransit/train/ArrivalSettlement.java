package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.economy.Rewards;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Alighting decisions and payouts for one arrival. Kept free of {@code Train} so it can be driven
 * from a command for integration checks.
 */
public final class ArrivalSettlement {

    public record Result(long total, int alighted, int stayed, Set<GlobalPos> origins) {
    }

    private ArrivalSettlement() {
    }

    public static Result settle(TrainManifest manifest, StationRecord village, GlobalPos villageKey,
                                long now, VTConfig.Snapshot cfg, VillageTransitData data) {
        long total = 0L;
        int alighted = 0;
        int stayed = 0;
        Set<GlobalPos> origins = new HashSet<>();

        for (Iterator<Map.Entry<TrainManifest.SeatKey, ItemStack>> iterator = manifest.seats().entrySet().iterator();
             iterator.hasNext(); ) {
            Map.Entry<TrainManifest.SeatKey, ItemStack> entry = iterator.next();
            if (Travellers.isMarker(entry.getValue())) {
                // A player is sitting here; the seat stays occupied.
                stayed++;
                continue;
            }
            PassengerData passenger = Travellers.data(entry.getValue());
            if (passenger == null) {
                iterator.remove();
                continue;
            }
            ItemStack aged = Travellers.ageOnBoard(entry.getValue());
            boolean patienceLost = aged == null;
            PassengerData after = patienceLost ? passenger.withPatience(0) : Travellers.data(aged);
            boolean wantsOff = patienceLost || Rewards.wantsOff(after.purpose(), village, cfg);
            if (!wantsOff) {
                entry.setValue(aged);
                stayed++;
                continue;
            }
            double distance = distanceBetween(passenger.origin(), villageKey, cfg);
            // No income for short hops: coming back to (or near) the origin pays nothing.
            if (distance > cfg.freeDistance) {
                double routeFactor = data.currentRouteFactor(passenger.origin(), villageKey, now, cfg);
                total += Rewards.payout(after.purpose(), after.patience(), distance, routeFactor, cfg);
            }
            origins.add(passenger.origin());
            iterator.remove();
            alighted++;
        }
        return new Result(total, alighted, stayed, origins);
    }

    public static double distanceBetween(GlobalPos from, GlobalPos to, VTConfig.Snapshot cfg) {
        if (from == null || to == null) {
            return 0.0;
        }
        if (!from.dimension().equals(to.dimension())) {
            return cfg.crossDimensionDistance;
        }
        return Math.sqrt(from.pos().distSqr(to.pos()));
    }
}
