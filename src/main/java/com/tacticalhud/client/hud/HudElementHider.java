package com.tacticalhud.client.hud;

import com.tacticalhud.TacticalHud;
import com.tacticalhud.config.HudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * While the 3D HUD is shown, hides TaCZ's classic gun HUD (which it replaces) and the vanilla HUD elements the
 * player chose to hide. Cancelling the overlay event keeps TaCZ itself untouched.
 */
@Mod.EventBusSubscriber(modid = TacticalHud.MOD_ID, value = Dist.CLIENT)
public final class HudElementHider {
    /** Registered by TaCZ via {@code RegisterGuiOverlaysEvent#registerAboveAll("tac_gun_hud_overlay", ...)}. */
    private static final ResourceLocation TACZ_GUN_HUD = new ResourceLocation("tacz", "tac_gun_hud_overlay");

    private HudElementHider() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation overlayId = event.getOverlay().id();
        boolean replaced = overlayId.equals(TACZ_GUN_HUD);
        ForgeConfigSpec.BooleanValue hideToggle = replaced ? null : hideToggleFor(overlayId);
        if (!replaced && (hideToggle == null || !hideToggle.get())) {
            return;
        }
        if (HudVisibility.shouldRender(Minecraft.getInstance())) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static ForgeConfigSpec.BooleanValue hideToggleFor(ResourceLocation overlayId) {
        if (overlayId.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || overlayId.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            return HudConfig.HIDE_HEALTH_ARMOR;
        }
        // The selected item name floats where the hotbar was, so it goes with it.
        if (overlayId.equals(VanillaGuiOverlay.HOTBAR.id()) || overlayId.equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            return HudConfig.HIDE_HOTBAR;
        }
        // The experience bar overlay also draws the level number.
        if (overlayId.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            return HudConfig.HIDE_EXPERIENCE;
        }
        if (overlayId.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            return HudConfig.HIDE_FOOD;
        }
        return null;
    }
}
