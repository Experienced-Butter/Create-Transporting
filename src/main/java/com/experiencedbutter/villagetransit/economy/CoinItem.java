package com.experiencedbutter.villagetransit.economy;

import net.minecraft.world.item.Item;

/** A coin of a fixed denomination. Balances are stored in cents, 10 cents = 1 copper coin. */
public class CoinItem extends Item {

    private final long faceValue;

    public CoinItem(Properties properties, long faceValue) {
        super(properties);
        this.faceValue = faceValue;
    }

    public long faceValue() {
        return faceValue;
    }
}
