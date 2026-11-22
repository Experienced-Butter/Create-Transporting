package com.experiencedbutter.villagetransit.economy;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** The seven denominations of the mod's currency. */
public final class Coins {

    public static final DeferredRegister.Items COIN_ITEMS =
            DeferredRegister.createItems(CreateVillageTransit.MOD_ID);

    public static final DeferredItem<CoinItem> COPPER = register("copper_coin", 10L);
    public static final DeferredItem<CoinItem> IRON = register("iron_coin", 100L);
    public static final DeferredItem<CoinItem> GOLD = register("gold_coin", 1_000L);
    public static final DeferredItem<CoinItem> AMETHYST = register("amethyst_coin", 10_000L);
    public static final DeferredItem<CoinItem> DIAMOND = register("diamond_coin", 100_000L);
    public static final DeferredItem<CoinItem> EMERALD = register("emerald_coin", 1_000_000L);
    public static final DeferredItem<CoinItem> NETHERITE = register("netherite_coin", 10_000_000L);

    public static final List<DeferredItem<CoinItem>> ALL =
            List.of(COPPER, IRON, GOLD, AMETHYST, DIAMOND, EMERALD, NETHERITE);

    private static DeferredItem<CoinItem> register(String name, long value) {
        return COIN_ITEMS.register(name, () -> new CoinItem(new Item.Properties(), value));
    }

    /** Largest denomination that still fits into the amount. */
    public static CoinItem largest(int startIndex, long amount) {
        for (int i = startIndex; i >= 0; i--) {
            CoinItem coin = ALL.get(i).get();
            if (coin.faceValue() <= amount) {
                return coin;
            }
        }
        return null;
    }

    private Coins() {
    }
}
