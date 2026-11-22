package com.experiencedbutter.villagetransit.economy;

import com.experiencedbutter.villagetransit.config.VTConfig;
import com.experiencedbutter.villagetransit.data.VillageTransitData;
import com.experiencedbutter.villagetransit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

/**
 * Holds the owner's wallet in item form.
 *
 * <p>The station has two modes, switched in the GUI: deposit (coins put into the slots are
 * converted into balance) and withdraw (balance is minted into coins). Conversion only happens
 * while the block receives a redstone signal.</p>
 *
 * <p>Safety model: coins in the slots are always already paid for. Minting is the only way to
 * create a coin and it deducts the balance in the same server-thread call.</p>
 */
public class TradeStationBlockEntity extends BlockEntity {

    public final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof CoinItem;
        }
    };

    /** false = coins become balance, true = balance becomes coins. */
    public boolean withdrawMode;
    public UUID owner;

    private int tickCounter;

    public TradeStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADE_STATION.get(), pos, state);
        this.withdrawMode = VTConfig.get().withdrawModeDefault;
    }

    public boolean isPowered(ServerLevel level) {
        return level.hasNeighborSignal(worldPosition);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, TradeStationBlockEntity station) {
        VillageTransitData data = VillageTransitData.get(level.getServer());
        if (!level.hasNeighborSignal(pos)) {
            // Redstone gates every conversion, so an unpowered station is inert.
            return;
        }
        if (station.withdrawMode) {
            if (++station.tickCounter < VTConfig.get().refillIntervalTicks) {
                return;
            }
            station.tickCounter = 0;
            station.refill(data);
        } else {
            station.deposit(data);
        }
    }

    /** Coins in the slots are consumed and credited to the owner. */
    public void deposit(VillageTransitData data) {
        if (owner == null) {
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof CoinItem coin) {
                data.addBalance(owner, coin.faceValue() * stack.getCount());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
            data.setDirty();
        }
    }

    /** Mints coins from the balance into the slots (largest denominations first). */
    public void refill(VillageTransitData data) {
        if (data == null || owner == null || level == null || level.isClientSide) {
            return;
        }
        VTConfig.Snapshot cfg = VTConfig.get();
        if (data.balance(owner) <= 0) {
            return;
        }
        int perSlot = cfg.maxMintPerRefill;
        for (int i = Coins.ALL.size() - 1; i >= 0; i--) {
            CoinItem coin = Coins.ALL.get(i).get();
            ItemStack current = inventory.getStackInSlot(i);
            if (!current.isEmpty() && current.getItem() != coin) {
                continue;
            }
            int room = coin.getDefaultMaxStackSize() - current.getCount();
            int maxCoins = (int) Math.min(Math.min(room, perSlot), data.balance(owner) / coin.faceValue());
            for (int c = 0; c < maxCoins; c++) {
                if (data.balance(owner) < coin.faceValue()) {
                    break;
                }
                data.addBalance(owner, -coin.faceValue());
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty()) {
                    inventory.setStackInSlot(i, new ItemStack(coin, 1));
                } else {
                    stack.grow(1);
                }
            }
        }
        setChanged();
        data.setDirty();
    }

    /** Mints a single coin of the given denomination into its slot, if the balance allows it. */
    public boolean mintOne(VillageTransitData data, int index) {
        if (data == null || owner == null || index < 0 || index >= Coins.ALL.size()) {
            return false;
        }
        CoinItem coin = Coins.ALL.get(index).get();
        long balance = data.balance(owner);
        if (balance < coin.faceValue()) {
            return false;
        }
        ItemStack current = inventory.getStackInSlot(index);
        if (!current.isEmpty() && current.getItem() != coin) {
            return false;
        }
        if (current.getCount() >= coin.getDefaultMaxStackSize()) {
            return false;
        }
        data.addBalance(owner, -coin.faceValue());
        if (current.isEmpty()) {
            inventory.setStackInSlot(index, new ItemStack(coin, 1));
        } else {
            current.grow(1);
        }
        setChanged();
        return true;
    }

    public void toggleMode() {
        withdrawMode = !withdrawMode;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        if (owner != null) {
            tag.put("Owner", NbtUtils.createUUID(owner));
        }
        tag.putBoolean("WithdrawMode", withdrawMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        owner = tag.contains("Owner") ? NbtUtils.loadUUID(tag.get("Owner")) : null;
        withdrawMode = tag.getBoolean("WithdrawMode");
    }
}
