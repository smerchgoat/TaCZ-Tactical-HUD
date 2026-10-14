package com.tacticalhud.client.hud;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacticalhud.config.HudConfig;
import com.tacticalhud.config.HudProfile;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Single source of truth for "is the 3D HUD replacing the classic gun HUD this frame", so the 3D HUD and
 * {@link HudElementHider} never disagree.
 */
public final class HudVisibility {
    private HudVisibility() {
    }

    public static boolean isEnabledForCamera(Minecraft mc) {
        if (!RenderConfig.GUN_HUD_ENABLE.get()) {
            return false;
        }
        return profileForCamera(mc).enabled.get();
    }

    public static HudProfile profileForCamera(Minecraft mc) {
        return mc.options.getCameraType().isFirstPerson() ? HudConfig.FIRST_PERSON : HudConfig.THIRD_PERSON;
    }

    public static boolean shouldRender(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!(player instanceof IClientPlayerGunOperator) || player.isSpectator()) {
            return false;
        }
        return player.getMainHandItem().getItem() instanceof IGun && isEnabledForCamera(mc);
    }
}
