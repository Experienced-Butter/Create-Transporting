package com.experiencedbutter.villagetransit.economy;

import com.experiencedbutter.villagetransit.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Trade station interface: seven coin slots, the player inventory and a deposit/withdraw switch.
 */
public class TradeStationMenu extends AbstractContainerMenu {

    public static final int BUTTON_TOGGLE_MODE = 300;
    public static final int COIN_SLOTS = 7;

    private final BlockPos pos;
    private final TradeStationBlockEntity station;
    private final SimpleContainerData data = new SimpleContainerData(4);
    private final IItemHandler coinHandler;

    public TradeStationMenu(int containerId, Inventory inventory, BlockPos pos, ItemStackHandler coins) {
        super(ModMenus.TRADE_STATION.get(), containerId);
        this.pos = pos;
        this.coinHandler = coins;
        this.station = inventory.player.level().getBlockEntity(pos) instanceof TradeStationBlockEntity be ? be : null;

        for (int i = 0; i < COIN_SLOTS; i++) {
            addSlot(new SlotItemHandler(coins, i, 8 + i * 20, 34));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public static TradeStationMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new TradeStationMenu(containerId, inventory, buffer.readBlockPos(), new ItemStackHandler(COIN_SLOTS));
    }

    public BlockPos pos() {
        return pos;
    }

    public long balance() {
        return ((long) data.get(1) << 32) | (data.get(0) & 0xFFFFFFFFL);
    }

    public boolean withdrawMode() {
        return (data.get(2) & 1) != 0;
    }

    public boolean powered() {
        return (data.get(2) & 2) != 0;
    }

    @Override
    public void broadcastChanges() {
        if (station != null && station.getLevel() instanceof ServerLevel level) {
            long balance = com.experiencedbutter.villagetransit.data.VillageTransitData
                    .get(level.getServer()).balance(station.owner);
            data.set(0, (int) balance);
            data.set(1, (int) (balance >>> 32));
            int flags = (station.withdrawMode ? 1 : 0) | (station.isPowered(level) ? 2 : 0);
            data.set(2, flags);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (station == null || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }
        if (id == BUTTON_TOGGLE_MODE) {
            station.toggleMode();
            // Apply the new mode immediately when the station is powered.
            var data = com.experiencedbutter.villagetransit.data.VillageTransitData
                    .get(serverPlayer.serverLevel().getServer());
            if (station.isPowered(serverPlayer.serverLevel())) {
                if (station.withdrawMode) {
                    station.refill(data);
                } else {
                    station.deposit(data);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0 * 64.0) {
            return false;
        }
        return player.level().getBlockState(pos).is(com.experiencedbutter.villagetransit.registry.ModBlocks.TRADE_STATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved;
        if (index < COIN_SLOTS) {
            moved = this.moveItemStackTo(stack, COIN_SLOTS, this.slots.size(), true);
        } else if (stack.getItem() instanceof CoinItem) {
            moved = this.moveItemStackTo(stack, 0, COIN_SLOTS, false);
        } else {
            return ItemStack.EMPTY;
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        slot.setChanged();
        if (slot.getItem().isEmpty()) {
            slot.set(ItemStack.EMPTY);
        }
        return copy;
    }

    /** Exposed so the screen can draw the same coin handler the server uses. */
    public IItemHandler coins() {
        return coinHandler;
    }

    @SuppressWarnings("unused")
    private static boolean isModifiable(IItemHandler handler) {
        return handler instanceof IItemHandlerModifiable;
    }
}
