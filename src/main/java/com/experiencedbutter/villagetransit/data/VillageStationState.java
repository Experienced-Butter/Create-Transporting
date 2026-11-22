package com.experiencedbutter.villagetransit.data;

public enum VillageStationState {
    /** Placed, but never successfully checked. */
    UNBOUND,
    /** Inside a village, running normally. */
    ACTIVE,
    /** No village structure / not enough POIs around it. */
    INVALID,
    /** Census could not run for a while (chunks unloaded); last known data is used. */
    STALE
}
