package com.experiencedbutter.villagetransit.economy;

/**
 * Pure payout maths. Deliberately free of Minecraft types so it can be unit tested on a plain JVM
 * and reused by both the arrival and the accident path.
 */
public final class RewardMath {

    private RewardMath() {
    }

    /** Distance multiplier with a dead zone: short shuttles get no bonus at all. */
    public static double distanceMultiplier(double distance, int freeDistance, int distanceK, double cap) {
        if (distance <= freeDistance) {
            return 1.0;
        }
        double excess = distance - freeDistance;
        return 1.0 + cap * (1.0 - Math.exp(-excess / Math.max(1, distanceK)));
    }

    public static double patienceMultiplier(int patience, double base, double perPoint) {
        return base + perPoint * Math.max(0, patience);
    }

    /** Repeated-route fatigue factor (1.0 = fresh route). */
    public static double routeFactor(double fatigue, boolean enabled, double k, double floor) {
        if (!enabled) {
            return 1.0;
        }
        return Math.max(floor, 1.0 / (1.0 + fatigue / k));
    }

    public static long payout(double base, int patience, double distance, double routeFactor,
                              int freeDistance, int distanceK, double cap,
                              double patienceBase, double patiencePerPoint) {
        double value = base
                * patienceMultiplier(patience, patienceBase, patiencePerPoint)
                * distanceMultiplier(distance, freeDistance, distanceK, cap)
                * routeFactor;
        return Math.max(0L, Math.round(value));
    }
}
