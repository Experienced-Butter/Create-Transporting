package com.experiencedbutter.villagetransit.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravellerMathTest {

    private static final double[] EMPLOYED = {0.30, 0.10, 0.60};
    private static final double[] UNEMPLOYED = {0.30, 0.60, 0.10};
    private static final int[] PATIENCE = {1, 4, 7, 9, 6, 3, 2, 1};

    @Test
    void purposeProbabilitiesAlwaysSumToOne() {
        for (int villagers = 1; villagers <= 40; villagers++) {
            for (int employed = 0; employed <= villagers; employed++) {
                double[] p = TravellerMath.purposeProbabilities(villagers, employed, EMPLOYED, UNEMPLOYED);
                assertEquals(1.0, p[0] + p[1] + p[2], 1e-9);
                assertTrue(p[0] >= 0 && p[1] >= 0 && p[2] >= 0);
            }
        }
    }

    @Test
    void travelIsAlwaysThirtyPercent() {
        double[] allEmployed = TravellerMath.purposeProbabilities(20, 20, EMPLOYED, UNEMPLOYED);
        double[] noneEmployed = TravellerMath.purposeProbabilities(20, 0, EMPLOYED, UNEMPLOYED);
        assertEquals(0.30, allEmployed[0], 1e-9);
        assertEquals(0.30, noneEmployed[0], 1e-9);
    }

    @Test
    void employmentShiftsTradeAndRelocate() {
        double[] allEmployed = TravellerMath.purposeProbabilities(20, 20, EMPLOYED, UNEMPLOYED);
        double[] noneEmployed = TravellerMath.purposeProbabilities(20, 0, EMPLOYED, UNEMPLOYED);
        assertEquals(0.60, allEmployed[2], 1e-9);   // trade
        assertEquals(0.10, allEmployed[1], 1e-9);   // relocate
        assertEquals(0.60, noneEmployed[1], 1e-9);
        assertEquals(0.10, noneEmployed[2], 1e-9);
    }

    @Test
    void halfIdleHalfEmployedGivesTheDesignTable() {
        double[] p = TravellerMath.purposeProbabilities(10, 5, EMPLOYED, UNEMPLOYED);
        assertEquals(0.30, p[0], 1e-9);
        assertEquals(0.35, p[1], 1e-9);   // 0.10 + 0.50 * 0.5
        assertEquals(0.35, p[2], 1e-9);   // 0.60 - 0.50 * 0.5
    }

    @Test
    void pickPurposeRespectsThresholds() {
        double[] p = {0.30, 0.35, 0.35};
        assertEquals(0, TravellerMath.pickPurpose(0.0, p));
        assertEquals(0, TravellerMath.pickPurpose(0.299, p));
        assertEquals(1, TravellerMath.pickPurpose(0.30, p));
        assertEquals(1, TravellerMath.pickPurpose(0.649, p));
        assertEquals(2, TravellerMath.pickPurpose(0.65, p));
        assertEquals(2, TravellerMath.pickPurpose(0.999, p));
    }

    @Test
    void patienceTableIsBoundedAndCentredNearFour() {
        assertEquals(1, TravellerMath.pickPatience(0.0, PATIENCE));
        assertEquals(8, TravellerMath.pickPatience(0.999, PATIENCE));
        double mean = TravellerMath.meanPatience(PATIENCE);
        assertTrue(mean > 4.0 && mean < 4.5, "mean was " + mean);
    }

    @Test
    void expectedCountUsesFloorPlusFraction() {
        assertEquals(2, TravellerMath.expectedCount(0.1, 20, 0.0));   // exactly 2
        assertEquals(3, TravellerMath.expectedCount(0.1, 25, 0.49));  // 2.5 -> bonus rolls in
        assertEquals(2, TravellerMath.expectedCount(0.1, 25, 0.51));  // 2.5 -> no bonus
        assertEquals(0, TravellerMath.expectedCount(0.0, 100, 0.9));
    }

    @Test
    void degenerateWeightsDoNotProduceNaN() {
        double[] p = TravellerMath.purposeProbabilities(0, 0, new double[] {0, 0, 0}, new double[] {0, 0, 0});
        assertEquals(1.0, p[0], 1e-9);
        assertEquals(1, TravellerMath.pickPatience(0.5, new int[] {0, 0, 0}));
    }
}
