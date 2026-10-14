package com.tacticalhud.client.hud;

import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Flat white pictograms drawn as geometry instead of textures: rectangles stay sharp at any angle and scale,
 * while a downscaled bitmap would shimmer under the perspective projection.
 */
final class HudIcons {
    static final int ICON_SIZE = 9;

    private static final String[] GRENADE = {
            ".....XX..",
            "...XXX.X.",
            "..XXXX...",
            ".XXXXXXX.",
            "XXXXXXXXX",
            "XX.XXXXXX",
            "XX.XXXXXX",
            ".XXXXXXX.",
            "..XXXXX..",
    };

    private static final String[] MEDKIT = {
            ".XXXXXXX.",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XX.....XX",
            "XXXX.XXXX",
            ".XXX.XXX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
    };

    private static final int BULLET_WIDTH = 3;
    private static final int BULLET_HEIGHT = 7;
    private static final int BULLET_SPACING = 4;

    private HudIcons() {
    }

    static void grenade(GuiGraphics graphics, int x, int y, int color) {
        bitmap(graphics, GRENADE, x, y, color);
    }

    static void medkit(GuiGraphics graphics, int x, int y, int color) {
        bitmap(graphics, MEDKIT, x, y, color);
    }

    /** Bullets side by side: one for semi, three for burst, three plus a feed bar for automatic. */
    static void fireMode(GuiGraphics graphics, FireMode mode, int centerX, int y, int color) {
        if (mode == FireMode.UNKNOWN) {
            return;
        }
        int bullets = mode == FireMode.SEMI ? 1 : 3;
        int width = (bullets - 1) * BULLET_SPACING + BULLET_WIDTH;
        int left = centerX - width / 2;
        for (int i = 0; i < bullets; i++) {
            bullet(graphics, left + i * BULLET_SPACING, y, color);
        }
        if (mode == FireMode.AUTO) {
            graphics.fill(left - 1, y + BULLET_HEIGHT + 1, left + width + 1, y + BULLET_HEIGHT + 2, color);
        }
    }

    private static void bullet(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 2, y + 1, color);
        graphics.fill(x, y + 1, x + BULLET_WIDTH, y + BULLET_HEIGHT - 2, color);
        graphics.fill(x, y + BULLET_HEIGHT - 1, x + BULLET_WIDTH, y + BULLET_HEIGHT, color);
    }

    /** Draws each horizontal run of 'X' as one rectangle to keep the quad count low. */
    private static void bitmap(GuiGraphics graphics, String[] rows, int x, int y, int color) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int runStart = -1;
            for (int column = 0; column <= line.length(); column++) {
                boolean filled = column < line.length() && line.charAt(column) == 'X';
                if (filled && runStart < 0) {
                    runStart = column;
                } else if (!filled && runStart >= 0) {
                    graphics.fill(x + runStart, y + row, x + column, y + row + 1, color);
                    runStart = -1;
                }
            }
        }
    }
}
