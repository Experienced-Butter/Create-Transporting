package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.economy.TradeStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Trade station: coin slots, deposit/withdraw switch and the player inventory. */
public class TradeStationScreen extends AbstractContainerScreen<TradeStationMenu> {

    private static final int PANEL = 0xFF2B2B2B;
    private static final int BORDER = 0xFFC9A227;
    private static final int HEADER = 0xFF3A3A3A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int SUBTLE = 0xFF9A9A9A;
    private static final int GOLD = 0xFFF2C641;
    private static final int BAD = 0xFFE05C5C;
    private static final int GOOD = 0xFF2FD27A;

    private static final int BUTTON_X = 8;
    private static final int BUTTON_Y = 58;
    private static final int BUTTON_W = 84;
    private static final int BUTTON_H = 14;

    public TradeStationScreen(TradeStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 18, HEADER);
        graphics.fill(x, y, x + imageWidth, y + 1, BORDER);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, BORDER);
        graphics.fill(x, y, x + 1, y + imageHeight, BORDER);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, BORDER);

        for (int i = 0; i < TradeStationMenu.COIN_SLOTS; i++) {
            slotBackground(graphics, x + 8 + i * 20, y + 34);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotBackground(graphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotBackground(graphics, x + 8 + col * 18, y + 142);
        }

        graphics.fill(x + BUTTON_X, y + BUTTON_Y, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + BUTTON_H, 0xFF4A5D3A);
        graphics.fill(x + BUTTON_X, y + BUTTON_Y, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + 1, BORDER);
        graphics.fill(x + BUTTON_X, y + BUTTON_Y + BUTTON_H - 1, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + BUTTON_H, BORDER);
    }

    private void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8A8A8A);
        graphics.fill(x, y, x + 16, y + 16, 0xFF1E1E1E);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        long balance = menu.balance();
        String amount = String.format("%d.%d", balance / 10, Math.abs(balance % 10));
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.trade.balance", amount),
                8, 20, balance < 0 ? BAD : GOLD, false);

        Component mode = menu.withdrawMode()
                ? Component.translatable("gui.create_village_transit.trade.mode.withdraw")
                : Component.translatable("gui.create_village_transit.trade.mode.deposit");
        graphics.drawString(this.font, mode, BUTTON_X + 6, BUTTON_Y + 3, TEXT, false);

        Component power = menu.powered()
                ? Component.translatable("gui.create_village_transit.trade.powered")
                : Component.translatable("gui.create_village_transit.trade.unpowered");
        graphics.drawString(this.font, power, 100, BUTTON_Y + 3, menu.powered() ? GOOD : SUBTLE, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (button == 0 && localX >= BUTTON_X && localX < BUTTON_X + BUTTON_W
                && localY >= BUTTON_Y && localY < BUTTON_Y + BUTTON_H) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        TradeStationMenu.BUTTON_TOGGLE_MODE);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
