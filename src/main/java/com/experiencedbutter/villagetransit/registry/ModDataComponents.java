package com.experiencedbutter.villagetransit.registry;

import com.experiencedbutter.villagetransit.CreateVillageTransit;
import com.experiencedbutter.villagetransit.passenger.PassengerData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
                    CreateVillageTransit.MOD_ID);

    public static final Supplier<DataComponentType<PassengerData>> PASSENGER =
            DATA_COMPONENTS.registerComponentType("passenger", builder -> builder
                    .persistent(PassengerData.CODEC)
                    .networkSynchronized(PassengerData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
