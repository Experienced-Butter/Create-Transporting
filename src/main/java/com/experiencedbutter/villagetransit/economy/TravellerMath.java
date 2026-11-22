package com.experiencedbutter.villagetransit.economy;

/**
 * Pure traveller generation maths (purpose weights, patience table, expected head count), kept
 * free of Minecraft types so the distributions can be unit tested.
 */
public final class TravellerMath {

    private TravellerMath() {
    }

    /**
     * Merged purpose probabilities in the order travel, relocate, trade. Employed and unemployed
     * villagers use different weight tables, so the result is a population weighted mix.
     */
    public static double[] purposeProbabilities(int villagers, int employed,
                                                double[] employedWeights, double[] unemployedWeights) {
        int total = Math.max(1, villagers);
        int jobs = Math.min(total, Math.max(0, employed));
        int idle = total - jobs;
        double[] probabilities = new double[3];
        for (int i = 0; i < 3; i++) {
            double employedWeight = i < employedWeights.length ? employedWeights[i] : 0;
            double unemployedWeight = i < unemployedWeights.length ? unemployedWeights[i] : 0;
            probabilities[i] = (jobs * employedWeight + idle * unemployedWeight) / total;
        }
        double sum = probabilities[0] + probabilities[1] + probabilities[2];
        if (sum <= 0) {
            return new double[] {1.0, 0.0, 0.0};
        }
        for (int i = 0; i < 3; i++) {
            probabilities[i] /= sum;
        }
        return probabilities;
    }

    /** @param roll uniform value in [0,1) @return index into the probability array */
    public static int pickPurpose(double roll, double[] probabilities) {
        double cursor = 0;
        double value = Math.max(0.0, Math.min(0.999999, roll));
        for (int i = 0; i < probabilities.length; i++) {
            cursor += probabilities[i];
            if (value < cursor) {
                return i;
            }
        }
        return probabilities.length - 1;
    }

    /** @param weights weights for patience 1..n @return patience in 1..n */
    public static int pickPatience(double roll, int[] weights) {
        int total = 0;
        for (int weight : weights) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return 1;
        }
        double value = Math.max(0.0, Math.min(0.999999, roll)) * total;
        double cursor = 0;
        for (int i = 0; i < weights.length; i++) {
            cursor += Math.max(0, weights[i]);
            if (value < cursor) {
                return i + 1;
            }
        }
        return weights.length;
    }

    /** Expected mean patience of the weight table (used by tests and the balance table). */
    public static double meanPatience(int[] weights) {
        double weighted = 0;
        double total = 0;
        for (int i = 0; i < weights.length; i++) {
            weighted += (double) (i + 1) * Math.max(0, weights[i]);
            total += Math.max(0, weights[i]);
        }
        return total <= 0 ? 1.0 : weighted / total;
    }

    /** Head count of a list update: floor(rate * villagers) plus a probabilistic extra one. */
    public static int expectedCount(double rate, int villagers, double roll) {
        double expected = Math.max(0.0, rate) * Math.max(0, villagers);
        int base = (int) Math.floor(expected);
        if (roll < expected - base) {
            base++;
        }
        return base;
    }
}
