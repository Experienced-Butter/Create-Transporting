package com.experiencedbutter.villagetransit.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure maths tests; they run without Minecraft on the classpath by design.
 */
class RewardMathTest {

    // defaults from VTConfig.Snapshot
    private static final int FREE_DISTANCE = 256;
    private static final int DISTANCE_K = 256;
    private static final double CAP_TRADE = 1.5;
    private static final double CAP_TRAVEL = 2.0;
    private static final double PATIENCE_BASE = 0.60;
    private static final double PATIENCE_PER_POINT = 0.15;

    @Test
    void shortRoutesGetNoDistanceBonus() {
        assertEquals(1.0, RewardMath.distanceMultiplier(50, FREE_DISTANCE, DISTANCE_K, CAP_TRADE), 1e-9);
        assertEquals(1.0, RewardMath.distanceMultiplier(FREE_DISTANCE, FREE_DISTANCE, DISTANCE_K, CAP_TRADE), 1e-9);
    }

    @Test
    void distanceBonusGrowsAndSaturates() {
        double near = RewardMath.distanceMultiplier(FREE_DISTANCE + 128, FREE_DISTANCE, DISTANCE_K, CAP_TRAVEL);
        double far = RewardMath.distanceMultiplier(FREE_DISTANCE + 8192, FREE_DISTANCE, DISTANCE_K, CAP_TRAVEL);
        assertTrue(near > 1.0);
        assertTrue(far > near);
        assertTrue(far <= 1.0 + CAP_TRAVEL + 1e-6);
    }

    @Test
    void patienceRaisesThePayout() {
        long low = RewardMath.payout(35, 1, 512, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE, PATIENCE_BASE, PATIENCE_PER_POINT);
        long high = RewardMath.payout(35, 8, 512, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE, PATIENCE_BASE, PATIENCE_PER_POINT);
        assertTrue(high > low);
    }

    @Test
    void tradeBasePaysMoreThanRelocateBase() {
        long trade = RewardMath.payout(35, 4, 512, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE, PATIENCE_BASE, PATIENCE_PER_POINT);
        long relocate = RewardMath.payout(10, 4, 512, 1.0, FREE_DISTANCE, DISTANCE_K, 1.0, PATIENCE_BASE, PATIENCE_PER_POINT);
        assertTrue(trade > relocate);
    }

    @Test
    void routeFatigueStartsAtPointEightAndNeverDropsBelowFloor() {
        assertEquals(0.8, RewardMath.routeFactor(1.0, true, 4.0, 0.2), 1e-9);
        assertTrue(RewardMath.routeFactor(1000.0, true, 4.0, 0.2) >= 0.2 - 1e-9);
        assertEquals(1.0, RewardMath.routeFactor(1000.0, false, 4.0, 0.2), 1e-9);
    }

    @Test
    void payoutNeverGoesNegative() {
        assertEquals(0L, RewardMath.payout(0, 10, 10000, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE, PATIENCE_BASE, PATIENCE_PER_POINT));
    }

    @Test
    void knownValueMatchesTheDesignTable() {
        // trade, patience 5, 512 blocks -> ~92 cents in the balance table
        long value = RewardMath.payout(35, 5, 512, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE, PATIENCE_BASE, PATIENCE_PER_POINT);
        assertTrue(value >= 88 && value <= 96, "expected ~92, got " + value);
    }

    // ---------------------------------------------------------------- boundary cases

    @Test
    void degenerateDistanceSettingsNeverDivideByZero() {
        assertEquals(1.0, RewardMath.distanceMultiplier(0, 0, 0, 1.5), 1e-9);
        double value = RewardMath.distanceMultiplier(1000, 0, 0, 1.5);
        assertTrue(Double.isFinite(value));
        assertTrue(value >= 1.0 && value <= 2.5 + 1e-9);
    }

    @Test
    void zeroCapMeansNoDistanceBonusAtAll() {
        assertEquals(1.0, RewardMath.distanceMultiplier(100000, 0, 256, 0.0), 1e-9);
    }

    @Test
    void negativeOrZeroPatienceNeverReducesBelowTheBaseFactor() {
        assertEquals(PATIENCE_BASE, RewardMath.patienceMultiplier(0, PATIENCE_BASE, PATIENCE_PER_POINT), 1e-9);
        assertEquals(PATIENCE_BASE, RewardMath.patienceMultiplier(-5, PATIENCE_BASE, PATIENCE_PER_POINT), 1e-9);
    }

    @Test
    void routeFatigueWithTinyKAndZeroFloorStaysPositive() {
        double factor = RewardMath.routeFactor(1000, true, 0.1, 0.0);
        assertTrue(factor > 0.0 && factor < 0.01);
    }

    @Test
    void zeroBaseValuePaysNothing() {
        assertEquals(0L, RewardMath.payout(0, 10, 4096, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE,
                PATIENCE_BASE, PATIENCE_PER_POINT));
    }

    @Test
    void extremelyLongTripsStayUnderTheCap() {
        long huge = RewardMath.payout(35, 10, 1_000_000, 1.0, FREE_DISTANCE, DISTANCE_K, CAP_TRADE,
                PATIENCE_BASE, PATIENCE_PER_POINT);
        long atCap = Math.round(35 * (PATIENCE_BASE + PATIENCE_PER_POINT * 10) * (1.0 + CAP_TRADE));
        assertTrue(huge <= atCap + 1, "expected <= " + atCap + ", got " + huge);
    }
}
