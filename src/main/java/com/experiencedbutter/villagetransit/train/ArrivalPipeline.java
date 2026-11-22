package com.experiencedbutter.villagetransit.train;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.data.TrainManifest;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import net.minecraft.core.GlobalPos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything that happens when a train reaches a linked village station: settle the travellers that
 * get off, apply the payout caps, credit the owner and board the waiting villagers.
 *
 * <p>Deliberately free of {@code Train} so the whole money path can be driven by a command.</p>
 */
public final class ArrivalPipeline {

    public record Outcome(long paid, long credited, int alighted, int stayed, int boarded) {
    }

    private ArrivalPipeline() {
    }

    public static Outcome apply(VillageTransitData data, StationRecord village, GlobalPos villageKey,
                                TrainManifest manifest, List<TrainManifest.SeatKey> seats,
                                Set<TrainManifest.SeatKey> playerSeats, UUID owner,
                                long now, VTConfig.Snapshot cfg) {
        ArrivalSettlement.Result settlement = ArrivalSettlement.settle(manifest, village, villageKey, now, cfg, data);
        long paid = settlement.total();
        long credited = 0L;

        if (paid > 0) {
            for (GlobalPos origin : settlement.origins()) {
                if (origin != null) {
                    data.registerRouteArrival(origin, villageKey, now, cfg);
                }
            }
            if (cfg.maxPayoutPerArrival > 0) {
                paid = Math.min(paid, cfg.maxPayoutPerArrival);
            }
            if (cfg.hourlyCapPerStation > 0) {
                if (now - village.payoutWindowStartTick > 72_000L) {
                    village.payoutWindowStartTick = now;
                    village.payoutWindowSum = 0;
                }
                long room = cfg.hourlyCapPerStation - village.payoutWindowSum;
                paid = Math.max(0L, Math.min(paid, room));
                village.payoutWindowSum += paid;
            }
            if (owner != null && paid > 0) {
                data.addBalance(owner, paid);
                credited = paid;
            }
        }

        int boarded = SeatBoarding.board(manifest, seats, playerSeats, village);
        data.setDirty();
        return new Outcome(paid, credited, settlement.alighted(), settlement.stayed(), boarded);
    }
}
