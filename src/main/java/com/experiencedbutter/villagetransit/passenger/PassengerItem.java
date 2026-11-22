package com.experiencedbutter.villagetransit.passenger;

import com.experiencedbutter.villagetransit.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PassengerItem extends Item {

    public PassengerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PassengerData data = stack.get(ModDataComponents.PASSENGER.get());
        if (data == null) {
            return;
        }
        tooltip.add(Component.translatable("item.create_village_transit.passenger.purpose",
                        Component.translatable(data.purpose().getDescriptionId()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.create_village_transit.passenger.patience", data.patience())
                .withStyle(data.isUltra() ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (!com.experiencedbutter.villagetransit.passenger.Travellers.originText(data).isEmpty()) {
            tooltip.add(Component.translatable("item.create_village_transit.passenger.origin", com.experiencedbutter.villagetransit.passenger.Travellers.originText(data))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        PassengerData data = stack.get(ModDataComponents.PASSENGER.get());
        return data != null && data.isUltra();
    }
}
