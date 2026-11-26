package com.experiencedbutter.villagetransit.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only: registers the in-game config screen. This class must never be loaded on a dedicated
 * server, because it references client-only classes.
 */
public final class ClientConfigScreen {

    private ClientConfigScreen() {
    }

    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new ConfigurationScreen(mod, parent));
    }
}
