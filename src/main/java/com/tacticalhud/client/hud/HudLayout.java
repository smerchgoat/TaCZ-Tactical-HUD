package com.tacticalhud.client.hud;

import com.tacticalhud.config.HudGuiScale;
import com.tacticalhud.config.HudProfile;
import net.minecraft.util.Mth;

/**
 * A camera profile resolved for the current screen, so the first and third person layouts can be blended.
 *
 * @param scale local units per GUI pixel, already adapted to the screen and GUI scale setting
 */
record HudLayout(float anchorX, float anchorY, float scale, float yaw, float tilt, float fov,
                 float swayStrength, float backgroundOpacity) {
    /**
     * GUI height (in GUI pixels) at which {@code Scale} 1 means one local unit per GUI pixel in AUTO mode.
     * Sizing against the screen height keeps the panel the same share of the screen at any GUI scale.
     */
    private static final float REFERENCE_GUI_HEIGHT = 360f;

    static HudLayout of(HudProfile profile, int guiHeight, double windowGuiScale) {
        HudGuiScale mode = profile.guiScale.get();
        float sizing = mode == HudGuiScale.AUTO
                ? guiHeight / REFERENCE_GUI_HEIGHT
                // Xn: n framebuffer pixels per unit, whatever the GUI scale; GUI pixels are windowGuiScale wide.
                : (float) (mode.factor() / windowGuiScale);
        return new HudLayout(
                profile.anchorX.get().floatValue(),
                profile.anchorY.get().floatValue(),
                profile.scale.get().floatValue() * sizing,
                profile.yaw.get().floatValue(),
                profile.tilt.get().floatValue(),
                profile.fov.get().floatValue(),
                profile.swayStrength.get().floatValue(),
                profile.backgroundOpacity.get().floatValue());
    }

    static HudLayout lerp(HudLayout from, HudLayout to, float t) {
        if (t <= 0f) {
            return from;
        }
        if (t >= 1f) {
            return to;
        }
        return new HudLayout(
                Mth.lerp(t, from.anchorX, to.anchorX),
                Mth.lerp(t, from.anchorY, to.anchorY),
                Mth.lerp(t, from.scale, to.scale),
                Mth.lerp(t, from.yaw, to.yaw),
                Mth.lerp(t, from.tilt, to.tilt),
                Mth.lerp(t, from.fov, to.fov),
                Mth.lerp(t, from.swayStrength, to.swayStrength),
                Mth.lerp(t, from.backgroundOpacity, to.backgroundOpacity));
    }
}
