package com.tacticalhud.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client config, stored in {@code config/tacz_tactical_hud-client.toml}. */
public final class HudConfig {
    /** Beside the gun, turned so the left edge recedes toward the sights and the right edge faces the player. */
    private static final HudProfile.Defaults FIRST_PERSON_DEFAULTS = new HudProfile.Defaults(
            true, HudGuiScale.AUTO, 0.72, 0.62, 1.25, -18, -6, 40, 1, 0.72);
    /** Beside the character's hip, turned so the right edge recedes, as in The Division. */
    private static final HudProfile.Defaults THIRD_PERSON_DEFAULTS = new HudProfile.Defaults(
            true, HudGuiScale.AUTO, 0.70, 0.62, 1.25, 18, -6, 40, 1, 0.72);

    public static final ForgeConfigSpec.BooleanValue HIDE_HEALTH_ARMOR;
    public static final ForgeConfigSpec.BooleanValue HIDE_HOTBAR;
    public static final ForgeConfigSpec.BooleanValue HIDE_EXPERIENCE;
    public static final ForgeConfigSpec.BooleanValue HIDE_FOOD;
    public static final HudProfile FIRST_PERSON;
    public static final HudProfile THIRD_PERSON;
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        builder.comment("Hide vanilla health and armor while the 3D HUD is shown, since the HUD displays them");
        HIDE_HEALTH_ARMOR = builder.define("HideHealthAndArmor", true);

        builder.comment("Hide the hotbar and the selected item name while the 3D HUD is shown");
        HIDE_HOTBAR = builder.define("HideHotbar", false);

        builder.comment("Hide the experience bar and level while the 3D HUD is shown");
        HIDE_EXPERIENCE = builder.define("HideExperience", false);

        builder.comment("Hide the food bar while the 3D HUD is shown");
        HIDE_FOOD = builder.define("HideFood", false);

        builder.pop();

        FIRST_PERSON = new HudProfile(builder, "first_person", "first person", FIRST_PERSON_DEFAULTS);
        THIRD_PERSON = new HudProfile(builder, "third_person", "third person", THIRD_PERSON_DEFAULTS);
        SPEC = builder.build();
    }

    private HudConfig() {
    }
}
