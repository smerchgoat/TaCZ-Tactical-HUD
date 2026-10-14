package com.tacticalhud.client.hud;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** Colors of The Division's SHD interface: off-white elements, signature orange, translucent dark glass. */
final class HudPalette {
    static final int WHITE = 0xFFF2F2F2;
    static final int TEXT_DIM = 0xFFB9BDC2;
    static final int ORANGE = 0xFFFF6D10;
    static final int RED = 0xFFE8343A;
    static final int PANEL = 0xFF101317;
    static final int TRACK = 0x40FFFFFF;
    static final int EMPTY_SEGMENT = 0x33FFFFFF;
    static final int DIVIDER = 0x4DFFFFFF;
    static final int BRACKET = 0xB3FFFFFF;
    static final int GHOST = 0xD9FFFFFF;

    /** Font#drawString treats an alpha below 4 as fully opaque, so text must be skipped below this. */
    static final int MIN_TEXT_ALPHA = 5;

    private HudPalette() {
    }

    static int fade(int argb, float alpha) {
        int a = Mth.clamp(Math.round(FastColor.ARGB32.alpha(argb) * alpha), 0, 255);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    static int lerp(int from, int to, float t) {
        return FastColor.ARGB32.lerp(Mth.clamp(t, 0f, 1f), from, to);
    }

    /** Smooth 0..1 pulse used for warnings. */
    static float pulse(float time, float frequency) {
        return 0.5f + 0.5f * Mth.sin(time * frequency * Mth.TWO_PI);
    }
}
