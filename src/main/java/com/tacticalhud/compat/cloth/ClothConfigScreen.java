package com.tacticalhud.compat.cloth;

import com.tacticalhud.config.HudConfig;
import com.tacticalhud.config.HudGuiScale;
import com.tacticalhud.config.HudProfile;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Locale;
import java.util.function.DoubleFunction;

/**
 * Settings screen built with Cloth Config. Only loaded when Cloth Config is installed, so this is the only class
 * allowed to touch its API.
 */
public final class ClothConfigScreen {
    private static final String KEY = "config.tacz_tactical_hud";

    private ClothConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder root = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(KEY))
                .setSavingRunnable(HudConfig.SPEC::save);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();
        ConfigCategory category = root.getOrCreateCategory(Component.translatable(KEY));

        category.addEntry(toggle(entryBuilder, "hide_health_armor", HudConfig.HIDE_HEALTH_ARMOR, true));
        category.addEntry(toggle(entryBuilder, "hide_hotbar", HudConfig.HIDE_HOTBAR, false));
        category.addEntry(toggle(entryBuilder, "hide_experience", HudConfig.HIDE_EXPERIENCE, false));
        category.addEntry(toggle(entryBuilder, "hide_food", HudConfig.HIDE_FOOD, false));

        category.addEntry(profile(entryBuilder, "first_person", HudConfig.FIRST_PERSON));
        category.addEntry(profile(entryBuilder, "third_person", HudConfig.THIRD_PERSON));
        return root.build();
    }

    private static AbstractConfigListEntry<?> profile(ConfigEntryBuilder entryBuilder, String name, HudProfile profile) {
        HudProfile.Defaults defaults = profile.defaults;
        SubCategoryBuilder sub = entryBuilder.startSubCategory(Component.translatable(KEY + "." + name)).setExpanded(true);

        sub.add(toggle(entryBuilder, "enabled", profile.enabled, defaults.enabled()));
        sub.add(entryBuilder.startEnumSelector(Component.translatable(KEY + ".gui_scale"), HudGuiScale.class, profile.guiScale.get())
                .setDefaultValue(defaults.guiScale())
                .setEnumNameProvider(ClothConfigScreen::guiScaleName)
                .setTooltip(Component.translatable(KEY + ".gui_scale.desc"))
                .setSaveConsumer(profile.guiScale::set).build());

        sub.add(slider(entryBuilder, "anchor_x", profile.anchorX, HudProfile.ANCHOR_MIN, HudProfile.ANCHOR_MAX,
                defaults.anchorX(), 1000, v -> format("%.1f%%", v * 100)));
        sub.add(slider(entryBuilder, "anchor_y", profile.anchorY, HudProfile.ANCHOR_MIN, HudProfile.ANCHOR_MAX,
                defaults.anchorY(), 1000, v -> format("%.1f%%", v * 100)));
        sub.add(slider(entryBuilder, "scale", profile.scale, HudProfile.SCALE_MIN, HudProfile.SCALE_MAX,
                defaults.scale(), 100, v -> format("%.2fx", v)));
        sub.add(slider(entryBuilder, "yaw", profile.yaw, HudProfile.YAW_MIN, HudProfile.YAW_MAX,
                defaults.yaw(), 10, v -> format("%.1f°", v)));
        sub.add(slider(entryBuilder, "tilt", profile.tilt, HudProfile.TILT_MIN, HudProfile.TILT_MAX,
                defaults.tilt(), 10, v -> format("%.1f°", v)));
        sub.add(slider(entryBuilder, "fov", profile.fov, HudProfile.FOV_MIN, HudProfile.FOV_MAX,
                defaults.fov(), 1, v -> format("%.0f°", v)));
        sub.add(slider(entryBuilder, "sway_strength", profile.swayStrength, HudProfile.SWAY_MIN, HudProfile.SWAY_MAX,
                defaults.swayStrength(), 100, v -> format("%.2f", v)));
        sub.add(slider(entryBuilder, "background_opacity", profile.backgroundOpacity, HudProfile.OPACITY_MIN, HudProfile.OPACITY_MAX,
                defaults.backgroundOpacity(), 100, v -> format("%.0f%%", v * 100)));
        return sub.build();
    }

    /**
     * Cloth Config only offers integer sliders, so the value is stored in steps of {@code 1 / stepsPerUnit}
     * and the label shows the real value.
     */
    private static AbstractConfigListEntry<?> slider(ConfigEntryBuilder entryBuilder, String name, ForgeConfigSpec.DoubleValue value,
                                                     double min, double max, double defaultValue, int stepsPerUnit,
                                                     DoubleFunction<String> label) {
        return entryBuilder.startIntSlider(Component.translatable(KEY + "." + name),
                        toSteps(value.get(), stepsPerUnit), toSteps(min, stepsPerUnit), toSteps(max, stepsPerUnit))
                .setDefaultValue(toSteps(defaultValue, stepsPerUnit))
                .setTextGetter(steps -> Component.literal(label.apply((double) steps / stepsPerUnit)))
                .setTooltip(Component.translatable(KEY + "." + name + ".desc"))
                .setSaveConsumer(steps -> value.set((double) steps / stepsPerUnit))
                .build();
    }

    private static AbstractConfigListEntry<?> toggle(ConfigEntryBuilder entryBuilder, String name, ForgeConfigSpec.BooleanValue value, boolean defaultValue) {
        return entryBuilder.startBooleanToggle(Component.translatable(KEY + "." + name), value.get())
                .setDefaultValue(defaultValue).setTooltip(Component.translatable(KEY + "." + name + ".desc"))
                .setSaveConsumer(value::set).build();
    }

    private static Component guiScaleName(Enum<?> scale) {
        return scale == HudGuiScale.AUTO
                ? Component.translatable(KEY + ".gui_scale.auto")
                : Component.literal("x" + ((HudGuiScale) scale).factor());
    }

    private static int toSteps(double value, int stepsPerUnit) {
        return (int) Math.round(value * stepsPerUnit);
    }

    private static String format(String pattern, double value) {
        return String.format(Locale.ROOT, pattern, value);
    }
}
