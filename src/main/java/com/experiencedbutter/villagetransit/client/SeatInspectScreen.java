package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.passenger.SeatInspectMenu;
import com.experiencedbutter.villagetransit.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Shows the traveller occupying a passenger seat. */
public class SeatInspectScreen extends AbstractContainerScreen<SeatInspectMenu> {

    private static final int PANEL = 0xFF2B2B2B;
    private static final int BORDER = 0xFFC9A227;
    private static final int HEADER = 0xFF3A3A3A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int SUBTLE = 0xFF9A9A9A;

    public SeatInspectScreen(SeatInspectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 96;
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
        graphics.fill(x + 14, y + 26, x + 50, y + 62, 0xFF1E1E1E);
        graphics.fill(x + 13, y + 25, x + 51, y + 27, 0xFF8A8A8A);
        graphics.fill(x + 13, y + 61, x + 51, y + 63, 0xFF8A8A8A);
        graphics.fill(x + 13, y + 25, x + 15, y + 63, 0xFF8A8A8A);
        graphics.fill(x + 49, y + 25, x + 51, y + 63, 0xFF8A8A8A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        ItemStack traveller = menu.traveller();
        PassengerData data = traveller.get(ModDataComponents.PASSENGER.get());
        graphics.renderItem(traveller, 24, 34);
        if (data == null) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.seat.empty"),
                    60, 36, SUBTLE, false);
            return;
        }
        int y = 30;
        graphics.drawString(this.font, Component.translatable(data.purpose().getDescriptionId())
                .withStyle(data.isUltra() ? ChatFormatting.AQUA : ChatFormatting.GOLD), 60, y, TEXT, false);
        y += 12;
        graphics.drawString(this.font, Component.translatable("item.create_village_transit.passenger.patience",
                data.patience()), 60, y, SUBTLE, false);
        y += 12;
        if (!com.experiencedbutter.villagetransit.passenger.Travellers.originText(data).isEmpty()) {
            graphics.drawString(this.font, Component.translatable("item.create_village_transit.passenger.origin",
                    com.experiencedbutter.villagetransit.passenger.Travellers.originText(data)), 60, y, SUBTLE, false);
        }
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.seat.hint"),
                8, 74, SUBTLE, false);
    }
}
