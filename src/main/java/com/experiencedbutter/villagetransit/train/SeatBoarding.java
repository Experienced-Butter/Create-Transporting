package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.TrainManifest;

import java.util.List;
import java.util.Set;

/**
 * Fills empty passenger seats from a village list, in seat order, skipping seats a player sits on.
 * Separated from {@link TrainTracker} so the rule can be exercised without a real train.
 */
public final class SeatBoarding {

    private SeatBoarding() {
    }

    /** @return how many travellers got on */
    public static int board(TrainManifest manifest, List<TrainManifest.SeatKey> seats,
                            Set<TrainManifest.SeatKey> playerSeats, StationRecord village) {
        int boarded = 0;
        for (TrainManifest.SeatKey seat : seats) {
            if (village.passengers.isEmpty()) {
                break;
            }
            if (playerSeats.contains(seat) || manifest.seats().containsKey(seat)) {
                continue;
            }
            manifest.seats().put(seat, village.passengers.remove(0));
            boarded++;
        }
        return boarded;
    }
}
