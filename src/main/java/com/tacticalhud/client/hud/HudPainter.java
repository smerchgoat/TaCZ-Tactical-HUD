package com.tacticalhud.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacticalhud.TacticalHud;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Lays out the panel in local units (one unit = one GUI pixel before {@code Scale}), already placed in 3D by
 * {@link HudPerspective}. Layout, left to right:
 * <pre>
 *  ┃┊  [■][■][■][■][■][■][■][■][■][■]   armor segments
 *  ┃┊  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫   health
 *  ┃┊ ┌─────────┬────┬───────┬───────┐
 *  ┃┊ │   46    │ 31 │ fire  │  gun  │   magazine | other guns | skills
 *  ┃┊ │  1176   │ 16 ├───────┼───────┤
 *  ┃┊ │         │▐02▌│ ● 02  │ ✚ 04  │   reserve  | spare mags | grenades, medkits
 *  ┗┛ └─────────┴────┴───────┴───────┘
 *  gauge + magazine ticks
 * </pre>
 */
final class HudPainter {
    static final int PANEL_WIDTH = 148;
    static final int CONTENT_LEFT = -15;
    static final int CONTENT_BOTTOM = 62;

    private static final Style FONT = Style.EMPTY.withFont(new ResourceLocation(TacticalHud.MOD_ID, "hud"));

    private static final int SEGMENTS = 10;
    private static final int SEGMENT_GAP = 2;
    private static final int SEGMENT_WIDTH = (PANEL_WIDTH - (SEGMENTS - 1) * SEGMENT_GAP) / SEGMENTS;
    private static final int SEGMENT_HEIGHT = 8;
    private static final int ARMOR_PER_SEGMENT = 2;

    private static final int HEALTH_Y = 11;
    private static final int HEALTH_HEIGHT = 3;
    private static final float LOW_HEALTH = 0.3f;

    private static final int BODY_Y = 18;
    private static final int BODY_BOTTOM = CONTENT_BOTTOM;
    private static final int AMMO_CELL_RIGHT = 52;
    private static final int COLUMN_LEFT = 54;
    private static final int COLUMN_RIGHT = 76;
    private static final int SKILL_LEFT = 78;
    private static final int SKILL_SLOT_WIDTH = (PANEL_WIDTH - SKILL_LEFT) / 2;
    private static final int SKILL_ROW_SPLIT = BODY_Y + 22;
    private static final int BRACKET_LENGTH = 4;

    private static final int GAUGE_LEFT = CONTENT_LEFT;
    private static final int GAUGE_RIGHT = GAUGE_LEFT + 2;
    private static final int TICKS_LEFT = -10;
    private static final int TICKS_RIGHT = -4;
    private static final int COLUMN_TOP = -2;
    private static final int MAX_MAG_TICKS = 30;

    private static final int GUN_ICON_TEXTURE_WIDTH = 39;
    private static final int GUN_ICON_TEXTURE_HEIGHT = 13;
    private static final int GUN_ICON_WIDTH = 32;
    private static final int GUN_ICON_HEIGHT = 11;

    /** Depth offsets between stacked elements; must exceed depth precision at the panel distance. */
    private static final float LAYER_FILL = 0.6f;
    private static final float LAYER_TOP = 1.2f;
    /** Sub-unit precision for smoothly moving bars, as GuiGraphics#fill only takes integers. */
    private static final float FINE = 16f;

    private GuiGraphics graphics;
    private Font font;
    private HudSnapshot hud;
    private HudAnimator anim;
    private float alpha;
    private float time;

    void paint(GuiGraphics graphics, Font font, HudSnapshot hud, HudAnimator anim, float alpha, float backgroundOpacity) {
        this.graphics = graphics;
        this.font = font;
        this.hud = hud;
        this.anim = anim;
        this.alpha = alpha;
        this.time = anim.time();

        graphics.drawManaged(() -> {
            paintBody(backgroundOpacity);
            paintArmor();
            paintHealth();
            paintMagazineColumn();
            if (anim.boot() > 0.55f) {
                paintAmmoCell();
                paintWeaponColumn();
                paintConsumables();
                paintFireMode();
            }
        });
        // Textured blits bypass the managed buffers, so they go after the batched geometry is flushed.
        if (anim.boot() > 0.55f) {
            paintGunSlot();
        }
        this.graphics = null;
        this.hud = null;
        this.anim = null;
    }

    private void paintBody(float backgroundOpacity) {
        int wipeRight = Math.round(PANEL_WIDTH * easeOut(Mth.clamp(anim.boot() / 0.6f, 0f, 1f)));
        if (wipeRight <= 0) {
            return;
        }
        fill(0, BODY_Y, wipeRight, BODY_BOTTOM, HudPalette.fade(HudPalette.PANEL, backgroundOpacity), 0);
        brackets(0, BODY_Y, wipeRight, BODY_BOTTOM);
        if (wipeRight < PANEL_WIDTH) {
            return;
        }
        fill(AMMO_CELL_RIGHT, BODY_Y + 4, AMMO_CELL_RIGHT + 1, BODY_BOTTOM - 4, HudPalette.DIVIDER, LAYER_FILL);
        fill(SKILL_LEFT - 1, BODY_Y + 4, SKILL_LEFT, BODY_BOTTOM - 4, HudPalette.DIVIDER, LAYER_FILL);
        fill(SKILL_LEFT + 3, SKILL_ROW_SPLIT, PANEL_WIDTH - 3, SKILL_ROW_SPLIT + 1, HudPalette.DIVIDER, LAYER_FILL);
        int slotSplit = SKILL_LEFT + SKILL_SLOT_WIDTH;
        fill(slotSplit, BODY_Y + 4, slotSplit + 1, SKILL_ROW_SPLIT - 3, HudPalette.DIVIDER, LAYER_FILL);
        fill(slotSplit, SKILL_ROW_SPLIT + 4, slotSplit + 1, BODY_BOTTOM - 4, HudPalette.DIVIDER, LAYER_FILL);
    }

    private void brackets(int left, int top, int right, int bottom) {
        int c = HudPalette.BRACKET;
        int l = BRACKET_LENGTH;
        fill(left, top, left + l, top + 1, c, LAYER_FILL);
        fill(left, top, left + 1, top + l, c, LAYER_FILL);
        fill(right - l, top, right, top + 1, c, LAYER_FILL);
        fill(right - 1, top, right, top + l, c, LAYER_FILL);
        fill(left, bottom - 1, left + l, bottom, c, LAYER_FILL);
        fill(left, bottom - l, left + 1, bottom, c, LAYER_FILL);
        fill(right - l, bottom - 1, right, bottom, c, LAYER_FILL);
        fill(right - 1, bottom - l, right, bottom, c, LAYER_FILL);
    }

    private void paintArmor() {
        int armor = hud.armorValue();
        for (int i = 0; i < SEGMENTS; i++) {
            // Segments switch on one after another during the boot sweep.
            if (anim.boot() < 0.05f + i * 0.045f) {
                return;
            }
            int left = i * (SEGMENT_WIDTH + SEGMENT_GAP);
            int right = left + SEGMENT_WIDTH;
            int points = armor - i * ARMOR_PER_SEGMENT;
            if (points >= ARMOR_PER_SEGMENT) {
                fill(left, 0, right, SEGMENT_HEIGHT, HudPalette.WHITE, LAYER_FILL);
            } else if (points > 0) {
                int middle = left + SEGMENT_WIDTH / 2;
                fill(left, 0, middle, SEGMENT_HEIGHT, HudPalette.WHITE, LAYER_FILL);
                fill(middle, 0, right, SEGMENT_HEIGHT, HudPalette.EMPTY_SEGMENT, LAYER_FILL);
            } else {
                fill(left, 0, right, SEGMENT_HEIGHT, HudPalette.EMPTY_SEGMENT, LAYER_FILL);
            }
        }
    }

    private void paintHealth() {
        float reveal = easeOut(Mth.clamp((anim.boot() - 0.1f) / 0.5f, 0f, 1f));
        if (reveal <= 0f) {
            return;
        }
        float width = PANEL_WIDTH * reveal;
        float bottom = HEALTH_Y + HEALTH_HEIGHT;
        fineFill(0, HEALTH_Y, width, bottom, 0x99101214, 0);
        fineFill(0, HEALTH_Y, width * anim.ghostHealth(), bottom, HudPalette.GHOST, LAYER_FILL * 0.5f);
        int color = anim.shownHealth() < LOW_HEALTH
                ? HudPalette.lerp(HudPalette.ORANGE, HudPalette.RED, HudPalette.pulse(time, 2f))
                : HudPalette.ORANGE;
        fineFill(0, HEALTH_Y, width * anim.shownHealth(), bottom, color, LAYER_FILL);
        fineFill(width, HEALTH_Y - 2, width + 1, bottom + 2, HudPalette.WHITE, LAYER_FILL);
    }

    private void paintMagazineColumn() {
        float reveal = Mth.clamp((anim.boot() - 0.2f) / 0.5f, 0f, 1f);
        if (reveal <= 0f) {
            return;
        }
        float height = CONTENT_BOTTOM - COLUMN_TOP;
        float top = CONTENT_BOTTOM - height * easeOut(reveal);

        boolean warning = hud.isLowAmmo() || hud.overheatLocked();
        int ticks = Math.min(hud.magCapacity(), MAX_MAG_TICKS);
        int filledTicks = Mth.ceil((float) Math.min(hud.magAmmo(), hud.magCapacity()) * ticks / hud.magCapacity());
        float pitch = height / ticks;
        float tickHeight = Math.max(1f, pitch * 0.55f);
        int filledColor = warning ? HudPalette.RED : HudPalette.WHITE;
        for (int i = 0; i < ticks; i++) {
            float tickBottom = CONTENT_BOTTOM - i * pitch;
            if (tickBottom - tickHeight < top) {
                break;
            }
            int color = i < filledTicks ? filledColor : HudPalette.EMPTY_SEGMENT;
            fineFill(TICKS_LEFT, tickBottom - tickHeight, TICKS_RIGHT, tickBottom, color, LAYER_FILL);
        }

        fineFill(GAUGE_LEFT, top, GAUGE_RIGHT, CONTENT_BOTTOM, HudPalette.TRACK, 0);
        float gauge;
        int gaugeColor;
        if (hud.hasHeat()) {
            gauge = hud.heat();
            gaugeColor = HudPalette.lerp(HudPalette.ORANGE, HudPalette.RED, hud.heat());
        } else {
            gauge = Math.max(anim.reloadProgress(), 0f);
            gaugeColor = HudPalette.ORANGE;
        }
        if (gauge > 0f) {
            fineFill(GAUGE_LEFT, Math.max(top, CONTENT_BOTTOM - height * gauge), GAUGE_RIGHT, CONTENT_BOTTOM, gaugeColor, LAYER_FILL);
        }
        if (warning) {
            int pip = HudPalette.fade(HudPalette.RED, 0.4f + 0.6f * HudPalette.pulse(time, 3f));
            fill(GAUGE_LEFT, CONTENT_BOTTOM - 4, GAUGE_RIGHT, CONTENT_BOTTOM, pip, LAYER_TOP);
        }
        int capLeft = GAUGE_LEFT - 1;
        int capRight = TICKS_RIGHT + 1;
        fineFill(capLeft, top - 2, capRight, top - 1, HudPalette.BRACKET, LAYER_FILL);
        fill(capLeft, CONTENT_BOTTOM + 1, capRight, CONTENT_BOTTOM + 2, HudPalette.BRACKET, LAYER_FILL);
    }

    private void paintAmmoCell() {
        float center = AMMO_CELL_RIGHT / 2f;
        int color;
        if (hud.overheatLocked() || hud.magAmmo() == 0) {
            color = HudPalette.lerp(HudPalette.WHITE, HudPalette.RED, 0.5f + 0.5f * HudPalette.pulse(time, 2.5f));
        } else if (hud.isLowAmmo()) {
            color = HudPalette.RED;
        } else {
            color = HudPalette.WHITE;
        }
        float kick = 1f + 0.08f * anim.shotFlash();
        text(HudSnapshotFactory.twoDigits(hud.magAmmo()), center, BODY_Y + 3, 2.5f * kick, color, 0.5f);
        int reserveColor = "0".equals(hud.reserveText()) ? HudPalette.RED : HudPalette.TEXT_DIM;
        text(hud.reserveText(), center, BODY_Y + 29, 1.25f, reserveColor, 0.5f);
    }

    private void paintWeaponColumn() {
        float center = (COLUMN_LEFT + COLUMN_RIGHT) / 2f;
        for (int slot = 0; slot < HudSnapshot.OTHER_GUN_SLOTS; slot++) {
            String ammo = hud.otherGunAmmo(slot);
            int color = ammo == null ? HudPalette.fade(HudPalette.TEXT_DIM, 0.45f) : HudPalette.TEXT_DIM;
            text(ammo == null ? "--" : ammo, center, BODY_Y + 4 + slot * 13, 1.1f, color, 0.5f);
        }
        int boxTop = BODY_Y + 29;
        fill(COLUMN_LEFT + 1, boxTop, COLUMN_RIGHT - 1, boxTop + 12, HudPalette.ORANGE, LAYER_FILL);
        text(hud.spareMagsText(), center, boxTop + 2, 1.1f, HudPalette.WHITE, 0.5f);
    }

    private void paintFireMode() {
        int centerX = SKILL_LEFT + SKILL_SLOT_WIDTH / 2;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, LAYER_TOP);
        HudIcons.fireMode(graphics, hud.fireMode(), centerX, BODY_Y + 6, faded(HudPalette.WHITE));
        pose.popPose();
    }

    private void paintConsumables() {
        int y = SKILL_ROW_SPLIT + 6;
        consumable(SKILL_LEFT + 6, y, hud.grenades(), true);
        consumable(SKILL_LEFT + SKILL_SLOT_WIDTH + 6, y, hud.medkits(), false);
    }

    private void consumable(int x, int y, int count, boolean grenade) {
        int color = count > 0 ? HudPalette.WHITE : HudPalette.fade(HudPalette.TEXT_DIM, 0.45f);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, LAYER_TOP);
        if (grenade) {
            HudIcons.grenade(graphics, x, y, faded(color));
        } else {
            HudIcons.medkit(graphics, x, y, faded(color));
        }
        pose.popPose();
        text(HudSnapshotFactory.twoDigits(Math.min(count, 99)), x + HudIcons.ICON_SIZE + 3, y, 1.1f, color, 0f);
    }

    private void paintGunSlot() {
        int left = SKILL_LEFT + SKILL_SLOT_WIDTH + 1;
        int right = PANEL_WIDTH - 1;
        int top = BODY_Y + 2;
        int bottom = SKILL_ROW_SPLIT - 2;
        if (hud.overheatLocked()) {
            fill(left, top, right, bottom, HudPalette.fade(HudPalette.RED, 0.85f), LAYER_FILL);
        } else if (anim.reloadProgress() >= 0f) {
            fineFill(left, top, left + (right - left) * anim.reloadProgress(), bottom, HudPalette.fade(HudPalette.ORANGE, 0.8f), LAYER_FILL);
        }
        graphics.flush();

        int x = left + (right - left - GUN_ICON_WIDTH) / 2;
        int y = (top + bottom - GUN_ICON_HEIGHT) / 2;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, LAYER_TOP);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(hud.gunHudTexture(), x, y, GUN_ICON_WIDTH, GUN_ICON_HEIGHT, 0, 0,
                GUN_ICON_TEXTURE_WIDTH, GUN_ICON_TEXTURE_HEIGHT, GUN_ICON_TEXTURE_WIDTH, GUN_ICON_TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();
    }

    /**
     * @param anchor 0 = left aligned at x, 0.5 = centered on x, 1 = right aligned
     */
    private void text(String value, float x, float y, float scale, int color, float anchor) {
        int argb = faded(color);
        if (value.isEmpty() || (argb >>> 24) < HudPalette.MIN_TEXT_ALPHA) {
            return;
        }
        Component component = Component.literal(value).withStyle(FONT);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, LAYER_TOP);
        pose.scale(scale, scale, 1f);
        pose.translate(-font.width(component) * anchor, 0, 0);
        graphics.drawString(font, component, 0, 0, argb, false);
        pose.popPose();
    }

    private void fill(int x1, int y1, int x2, int y2, int color, float layer) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, layer);
        graphics.fill(x1, y1, x2, y2, faded(color));
        pose.popPose();
    }

    private void fineFill(float x1, float y1, float x2, float y2, int color, float layer) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, layer);
        pose.scale(1f / FINE, 1f / FINE, 1f);
        graphics.fill(Math.round(x1 * FINE), Math.round(y1 * FINE), Math.round(x2 * FINE), Math.round(y2 * FINE), faded(color));
        pose.popPose();
    }

    private int faded(int color) {
        return HudPalette.fade(color, alpha);
    }

    private static float easeOut(float t) {
        float inverse = 1f - t;
        return 1f - inverse * inverse * inverse;
    }
}
