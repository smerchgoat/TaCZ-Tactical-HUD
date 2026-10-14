package com.tacticalhud.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Layout of the 3D HUD for one camera view. First and third person each get their own instance. */
public final class HudProfile {
    public static final double ANCHOR_MIN = 0, ANCHOR_MAX = 1;
    public static final double SCALE_MIN = 0.25, SCALE_MAX = 4;
    public static final double YAW_MIN = -60, YAW_MAX = 60;
    public static final double TILT_MIN = -45, TILT_MAX = 45;
    public static final double FOV_MIN = 10, FOV_MAX = 110;
    public static final double SWAY_MIN = 0, SWAY_MAX = 3;
    public static final double OPACITY_MIN = 0, OPACITY_MAX = 1;

    public final Defaults defaults;
    public final ForgeConfigSpec.BooleanValue enabled;
    public final ForgeConfigSpec.EnumValue<HudGuiScale> guiScale;
    public final ForgeConfigSpec.DoubleValue anchorX;
    public final ForgeConfigSpec.DoubleValue anchorY;
    public final ForgeConfigSpec.DoubleValue scale;
    public final ForgeConfigSpec.DoubleValue yaw;
    public final ForgeConfigSpec.DoubleValue tilt;
    public final ForgeConfigSpec.DoubleValue fov;
    public final ForgeConfigSpec.DoubleValue swayStrength;
    public final ForgeConfigSpec.DoubleValue backgroundOpacity;

    HudProfile(ForgeConfigSpec.Builder builder, String section, String viewName, Defaults defaults) {
        this.defaults = defaults;
        builder.push(section);

        builder.comment("Whether or not to show the 3D HUD in " + viewName + " instead of the classic gun HUD");
        enabled = builder.define("Enabled", defaults.enabled());

        builder.comment("How the HUD size adapts to the screen: AUTO follows the screen height, X1..X4 render as if the GUI scale were 1..4");
        guiScale = builder.defineEnum("GuiScale", defaults.guiScale());

        builder.comment("Horizontal position of the HUD center, as a fraction of the screen width");
        anchorX = builder.defineInRange("AnchorX", defaults.anchorX(), ANCHOR_MIN, ANCHOR_MAX);

        builder.comment("Vertical position of the HUD center, as a fraction of the screen height");
        anchorY = builder.defineInRange("AnchorY", defaults.anchorY(), ANCHOR_MIN, ANCHOR_MAX);

        builder.comment("Size multiplier of the HUD");
        scale = builder.defineInRange("Scale", defaults.scale(), SCALE_MIN, SCALE_MAX);

        builder.comment("Rotation of the panel around the vertical axis, in degrees. Positive values push the right edge away from the camera, negative values the left edge");
        yaw = builder.defineInRange("Yaw", defaults.yaw(), YAW_MIN, YAW_MAX);

        builder.comment("Rotation of the panel around the horizontal axis, in degrees. Negative values push the top edge away from the camera");
        tilt = builder.defineInRange("Tilt", defaults.tilt(), TILT_MIN, TILT_MAX);

        builder.comment("Field of view of the HUD camera, in degrees. Lower values flatten the perspective");
        fov = builder.defineInRange("Fov", defaults.fov(), FOV_MIN, FOV_MAX);

        builder.comment("How strongly the HUD lags behind camera rotation, 0 disables the sway");
        swayStrength = builder.defineInRange("SwayStrength", defaults.swayStrength(), SWAY_MIN, SWAY_MAX);

        builder.comment("Opacity of the dark panel background");
        backgroundOpacity = builder.defineInRange("BackgroundOpacity", defaults.backgroundOpacity(), OPACITY_MIN, OPACITY_MAX);

        builder.pop();
    }

    public record Defaults(boolean enabled, HudGuiScale guiScale, double anchorX, double anchorY, double scale,
                           double yaw, double tilt, double fov, double swayStrength, double backgroundOpacity) {
    }
}
