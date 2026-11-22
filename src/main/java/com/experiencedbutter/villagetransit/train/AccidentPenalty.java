package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.economy.Rewards;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.Travellers;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * Money lost when a train carrying travellers crashes, derails or is dismantled: every traveller
 * is settled as if they had alighted at the accident location.
 */
public final class AccidentPenalty {

    private AccidentPenalty() {
    }

    public static long total(TrainManifest manifest, GlobalPos accident, VTConfig.Snapshot cfg) {
        long total = 0L;
        for (ItemStack stack : manifest.seats().values()) {
            PassengerData passenger = Travellers.data(stack);
            if (passenger == null) {
                continue;
            }
            double distance = ArrivalSettlement.distanceBetween(passenger.origin(), accident, cfg);
            total += Math.round(Rewards.payout(passenger.purpose(), passenger.patience(), distance, 1.0, cfg)
                    * cfg.accidentPenaltyFactor);
        }
        return total;
    }

    public static int count(TrainManifest manifest) {
        return manifest.size();
    }
}
