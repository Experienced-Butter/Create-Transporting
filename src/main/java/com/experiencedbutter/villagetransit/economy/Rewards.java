package com.experiencedbutter.villagetransit.economy;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.StationRecord;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.PassengerPurpose;

/**
 * Minecraft aware wrapper around {@link RewardMath}: resolves purposes and config values.
 */
public final class Rewards {

    private Rewards() {
    }

    /** Preference check used when a train arrives: should this traveller get off here? */
    public static boolean wantsOff(PassengerPurpose purpose, StationRecord village, VTConfig.Snapshot cfg) {
        return switch (purpose) {
            case TRAVEL -> village.villagers < cfg.travelBelow;
            case TRADE -> village.villagers > cfg.tradeAbove;
            case RELOCATE -> village.unemployedRatio() <= cfg.relocateMaxUnemployedRatio;
        };
    }

    public static double baseFor(PassengerPurpose purpose, VTConfig.Snapshot cfg) {
        return switch (purpose) {
            case TRAVEL -> cfg.baseTravel;
            case RELOCATE -> cfg.baseRelocate;
            case TRADE -> cfg.baseTrade;
        };
    }

    public static double capFor(PassengerPurpose purpose, VTConfig.Snapshot cfg) {
        return switch (purpose) {
            case TRAVEL -> cfg.capTravel;
            case RELOCATE -> cfg.capRelocate;
            case TRADE -> cfg.capTrade;
        };
    }

    public static double distanceMultiplier(double distance, PassengerPurpose purpose, VTConfig.Snapshot cfg) {
        return RewardMath.distanceMultiplier(distance, cfg.freeDistance, cfg.distanceK, capFor(purpose, cfg));
    }

    public static double patienceMultiplier(int patience, VTConfig.Snapshot cfg) {
        return RewardMath.patienceMultiplier(patience, cfg.patienceBase, cfg.patiencePerPoint);
    }

    public static long payout(PassengerPurpose purpose, int patience, double distance,
                              double routeFactor, VTConfig.Snapshot cfg) {
        return RewardMath.payout(baseFor(purpose, cfg), patience, distance, routeFactor,
                cfg.freeDistance, cfg.distanceK, capFor(purpose, cfg),
                cfg.patienceBase, cfg.patiencePerPoint);
    }

    public static long payout(PassengerData data, int patience, double distance, VTConfig.Snapshot cfg) {
        return payout(data.purpose(), patience, distance, 1.0, cfg);
    }

    public static double routeFactor(double fatigue, VTConfig.Snapshot cfg) {
        return RewardMath.routeFactor(fatigue, cfg.routeFatigueEnabled, cfg.routeFatigueK, cfg.routeFatigueFloor);
    }
}
