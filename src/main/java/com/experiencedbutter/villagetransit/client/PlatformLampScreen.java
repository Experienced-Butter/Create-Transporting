package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.network.LampBindPayload;
import com.experiencedbutter.villagetransit.network.LampDataPayload;
import com.experiencedbutter.villagetransit.village.PlatformLampMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlatformLampScreen extends AbstractContainerScreen<PlatformLampMenu> {

    private static final int PANEL = 0xFF2B2B2B;
    private static final int BORDER = 0xFFC9A227;
    private static final int HEADER = 0xFF3A3A3A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int SUBTLE = 0xFF9A9A9A;
    private static final int GOOD = 0xFF2FD27A;
    private static final int WARN = 0xFFE0B76A;

    private static final int ROW_Y = 32;
    private static final int ROW_H = 34;
    private static final int SLOT_X = 10;
    private static final int SLOT_SIZE = 20;

    private int ticks;

    public PlatformLampScreen(PlatformLampMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 120;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 1000;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ticks++;
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

        for (int i = 0; i < 2; i++) {
            int rowY = y + ROW_Y + i * ROW_H;
            graphics.fill(x + SLOT_X - 2, rowY - 2, x + SLOT_X + SLOT_SIZE + 2, rowY + SLOT_SIZE + 2, 0xFF3A3A3A);
            graphics.fill(x + SLOT_X - 1, rowY - 1, x + SLOT_X + SLOT_SIZE + 1, rowY + SLOT_SIZE + 1, 0xFF8A8A8A);
            graphics.fill(x + SLOT_X, rowY, x + SLOT_X + SLOT_SIZE, rowY + SLOT_SIZE, 0xFF1E1E1E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        LampDataPayload data = ClientLampCache.get(menu.pos());

        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.lamp.village"),
                40, ROW_Y + 2, TEXT, false);
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.lamp.station"),
                40, ROW_Y + ROW_H + 2, TEXT, false);

        if (data == null) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.loading"),
                    8, 96, SUBTLE, false);
            return;
        }

        if (data.hasVillage()) {
            graphics.renderItem(new ItemStack(com.experiencedbutter.villagetransit.registry.ModBlocks.VILLAGE_STATION.get()),
                    SLOT_X + 2, ROW_Y + 2);
            graphics.drawString(this.font, coords(data.villagePos()), 40, ROW_Y + 13, SUBTLE, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.lamp.click_to_bind"),
                    40, ROW_Y + 13, WARN, false);
        }

        if (data.hasStation()) {
            graphics.renderItem(new ItemStack(com.simibubi.create.AllBlocks.TRACK_STATION.get().asItem()),
                    SLOT_X + 2, ROW_Y + ROW_H + 2);
            graphics.drawString(this.font, coords(data.stationPos()), 40, ROW_Y + ROW_H + 13, SUBTLE, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.lamp.click_to_bind"),
                    40, ROW_Y + ROW_H + 13, WARN, false);
        }

        Component status = switch (data.status()) {
            case LampDataPayload.STATUS_COMPLETE -> Component.translatable("gui.create_village_transit.lamp.ready");
            case LampDataPayload.STATUS_NO_VILLAGE -> Component.translatable("gui.create_village_transit.lamp.need_village");
            case LampDataPayload.STATUS_NO_STATION -> Component.translatable("gui.create_village_transit.lamp.need_station");
            case LampDataPayload.STATUS_STATION_MISSING -> Component.translatable("gui.create_village_transit.lamp.station_missing");
            default -> Component.translatable("gui.create_village_transit.lamp.village_inactive");
        };
        graphics.drawString(this.font, status, 8, 96, data.status() == LampDataPayload.STATUS_COMPLETE ? GOOD : WARN, false);
    }

    private String coords(net.minecraft.core.BlockPos pos) {
        return pos == null ? "-" : pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (button == 0 && localX >= SLOT_X - 2 && localX <= SLOT_X + SLOT_SIZE + 2) {
            for (int i = 0; i < 2; i++) {
                int rowY = ROW_Y + i * ROW_H;
                if (localY >= rowY - 2 && localY <= rowY + SLOT_SIZE + 2) {
                    SelectionMode.start(menu.pos(), i);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                        this.minecraft.player.displayClientMessage(
                                Component.translatable("message.create_village_transit.pick_target"), true);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
