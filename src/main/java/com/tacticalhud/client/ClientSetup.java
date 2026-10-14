package com.tacticalhud.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacticalhud.TacticalHud;
import com.tacticalhud.client.hud.TacticalHudOverlay;
import com.tacticalhud.compat.cloth.ClothConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

/** Client-only wiring. Never referenced on a dedicated server. */
public final class ClientSetup {
    private static final String CLOTH_CONFIG_ID = "cloth_config";

    /** Unbound by default; the settings are also reachable from the Mods list. */
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.tacz_tactical_hud.open_settings",
            KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, "key.categories.tacz_tactical_hud");

    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerOverlays);
        modBus.addListener(ClientSetup::registerKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        if (isClothConfigLoaded()) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ClothConfigScreen.create(parent)));
        }
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("tactical_hud", new TacticalHudOverlay());
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_SETTINGS.consumeClick()) {
            if (isClothConfigLoaded()) {
                mc.setScreen(ClothConfigScreen.create(mc.screen));
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.tacz_tactical_hud.cloth_missing"), true);
            }
        }
    }

    private static boolean isClothConfigLoaded() {
        return ModList.get().isLoaded(CLOTH_CONFIG_ID);
    }
}
