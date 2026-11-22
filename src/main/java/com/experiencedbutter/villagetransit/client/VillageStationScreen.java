package com.experiencedbutter.villagetransit.client;

import com.experiencedbutter.villagetransit.data.VillageStationState;
import com.experiencedbutter.villagetransit.network.StationActionPayload;
import com.experiencedbutter.villagetransit.network.StationDataPayload;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import com.experiencedbutter.villagetransit.registry.ModDataComponents;
import com.experiencedbutter.villagetransit.village.VillageStationMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Create-flavoured station panel: village numbers on the left, the traveller list on the right and
 * a manual survey button at the bottom. All numbers come from the server.
 */
public class VillageStationScreen extends AbstractContainerScreen<VillageStationMenu> {

    private static final int PANEL = 0xFF2B2B2B;
    private static final int BORDER = 0xFFC9A227;
    private static final int HEADER = 0xFF3A3A3A;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int SUBTLE = 0xFF9A9A9A;
    private static final int GOOD = 0xFF2FD27A;
    private static final int WARN = 0xFFE0B76A;
    private static final int BAD = 0xFFE05C5C;

    private static final int ROW_HEIGHT = 18;
    private int scroll;
    private static final int ROWS = 6;
    private static final int LIST_X = 104;
    private static final int LIST_Y = 28;
    private static final int BUTTON_X = 8;
    private static final int BUTTON_Y = 144;
    private static final int BUTTON_W = 76;
    private static final int BUTTON_H = 14;

    private int ticks;
    private int refreshCooldown;

    public VillageStationScreen(VillageStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 192;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 1000;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ticks++;
        if (refreshCooldown > 0) {
            refreshCooldown--;
        }
        if (ticks == 1 || ticks % 20 == 0) {
            PacketDistributor.sendToServer(new StationActionPayload(menu.pos(), StationActionPayload.Action.REQUEST));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        StationDataPayload data = ClientStationCache.get(menu.pos());
        if (data != null) {
            List<Component> tooltip = passengerTooltip(data, mouseX, mouseY);
            if (tooltip != null) {
                graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(),
                        mouseX - this.leftPos, mouseY - this.topPos);
            }
        }
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
        graphics.fill(LIST_X - 4, y + 22, LIST_X - 3, y + imageHeight - 24, 0xFF4A4A4A);

        StationDataPayload data = ClientStationCache.get(menu.pos());
        int buttonColor = isRefreshEnabled(data) ? 0xFF4A5D3A : 0xFF3A3A3A;
        graphics.fill(x + BUTTON_X, y + BUTTON_Y, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + BUTTON_H, buttonColor);
        graphics.fill(x + BUTTON_X, y + BUTTON_Y, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + 1, BORDER);
        graphics.fill(x + BUTTON_X, y + BUTTON_Y + BUTTON_H - 1, x + BUTTON_X + BUTTON_W, y + BUTTON_Y + BUTTON_H, BORDER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        StationDataPayload data = ClientStationCache.get(menu.pos());
        if (data == null) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.loading"),
                    8, 30, SUBTLE, false);
            return;
        }

        VillageStationState state = VillageStationState.values()[Math.floorMod(data.state(), VillageStationState.values().length)];
        int stateColor = switch (state) {
            case ACTIVE -> GOOD;
            case INVALID -> BAD;
            case STALE, UNBOUND -> WARN;
        };
        int ly = 24;
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.state",
                Component.translatable("state.create_village_transit." + state.name().toLowerCase(java.util.Locale.ROOT))),
                8, ly, stateColor, false);
        ly += 12;
        // Only explain failures: an active station keeps working from its last census even when a
        // later survey could not run, so a stale reason there would just be noise.
        if (state != VillageStationState.ACTIVE && !data.reason().isEmpty()) {
            graphics.drawString(this.font, reasonText(data.reason()), 8, ly, WARN, false);
            ly += 12;
        }
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.villagers",
                data.villagers(), data.employed()), 8, ly, TEXT, false);
        ly += 12;
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.pois",
                data.workstations(), data.beds()), 8, ly, TEXT, false);
        ly += 12;
        graphics.drawString(this.font, Component.translatable("gui.create_village_transit.travellers",
                data.passengers().size(), data.capacity()), 8, ly, TEXT, false);
        ly += 12;
        if (data.secondsToUpdate() > 0) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.next_arrival",
                    data.secondsToUpdate()), 8, ly, SUBTLE, false);
            ly += 12;
        }
        if (data.hourlyCap() > 0) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.payout",
                    data.payoutWindowSum(), data.hourlyCap()), 8, ly,
                    data.payoutWindowSum() >= data.hourlyCap() ? BAD : SUBTLE, false);
            ly += 12;
        }
        if (data.refreshInFlight()) {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.refreshing"),
                    8, ly, WARN, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.create_village_transit.next",
                    data.secondsToCensus()), 8, ly, SUBTLE, false);
        }

        List<ItemStack> passengers = data.passengers();
        int maxScroll = Math.max(0, passengers.size() - ROWS);
        this.scroll = net.minecraft.util.Mth.clamp(this.scroll, 0, maxScroll);
        for (int i = 0; i < Math.min(ROWS, passengers.size() - this.scroll); i++) {
            ItemStack stack = passengers.get(i + this.scroll);
            PassengerData passenger = stack.get(ModDataComponents.PASSENGER.get());
            int rowY = LIST_Y + i * ROW_HEIGHT;
            graphics.renderItem(stack, LIST_X, rowY);
            if (passenger != null) {
                Component label = Component.translatable(passenger.purpose().getDescriptionId())
                        .withStyle(passenger.isUltra() ? ChatFormatting.AQUA : ChatFormatting.WHITE);
                graphics.drawString(this.font, label, LIST_X + 20, rowY + 4, TEXT, false);
                graphics.drawString(this.font, String.valueOf(passenger.patience()),
                        LIST_X + 74, rowY + 4, passenger.isUltra() ? 0xFF55FFFF : SUBTLE, false);
            }
        }
        if (passengers.size() > ROWS) {
            graphics.drawString(this.font, "+" + (passengers.size() - ROWS), LIST_X + 20,
                    LIST_Y + ROWS * ROW_HEIGHT, SUBTLE, false);
        }

        Component buttonLabel = data.refreshInFlight()
                ? Component.translatable("gui.create_village_transit.refreshing")
                : Component.translatable("gui.create_village_transit.refresh");
        graphics.drawString(this.font, buttonLabel, BUTTON_X + 6, BUTTON_Y + 3,
                isRefreshEnabled(data) ? TEXT : SUBTLE, false);
    }

    private static Component reasonText(String reason) {
        return switch (reason) {
            case "area_not_loaded", "not_in_village_structure", "not_enough_village_pois" ->
                    Component.translatable("reason.create_village_transit." + reason);
            default -> Component.literal(reason);
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            this.scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isRefreshEnabled(StationDataPayload data) {
        return data != null && !data.refreshInFlight() && refreshCooldown == 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        StationDataPayload data = ClientStationCache.get(menu.pos());
        double localX = mouseX - this.leftPos;
        double localY = mouseY - this.topPos;
        if (button == 0
                && localX >= BUTTON_X && localX < BUTTON_X + BUTTON_W
                && localY >= BUTTON_Y && localY < BUTTON_Y + BUTTON_H
                && isRefreshEnabled(data)) {
            PacketDistributor.sendToServer(new StationActionPayload(menu.pos(), StationActionPayload.Action.REFRESH));
            refreshCooldown = 20;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<Component> passengerTooltip(StationDataPayload data, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        if (localX < LIST_X || localX > LIST_X + 88) {
            return null;
        }
        int row = (localY - LIST_Y) / ROW_HEIGHT;
        if (row < 0 || row >= ROWS || localY < LIST_Y) {
            return null;
        }
        List<ItemStack> passengers = data.passengers();
        int index = row + this.scroll;
        if (index >= passengers.size()) {
            return null;
        }
        PassengerData passenger = passengers.get(index).get(ModDataComponents.PASSENGER.get());
        if (passenger == null) {
            return null;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(passenger.purpose().getDescriptionId()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.create_village_transit.passenger.patience", passenger.patience())
                .withStyle(ChatFormatting.GRAY));
        if (!com.experiencedbutter.villagetransit.passenger.Travellers.originText(passenger).isEmpty()) {
            lines.add(Component.translatable("item.create_village_transit.passenger.origin", com.experiencedbutter.villagetransit.passenger.Travellers.originText(passenger))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.translatable("gui.create_village_transit.hint." + passenger.purpose().getSerializedName())
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // handled in render()
    }
}
