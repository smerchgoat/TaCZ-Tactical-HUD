package com.tacticalhud.config;

/** How the 3D HUD size reacts to the screen: AUTO follows the screen height, Xn renders as if the GUI scale were n. */
public enum HudGuiScale {
    AUTO(0),
    X1(1),
    X2(2),
    X3(3),
    X4(4);

    private final int factor;

    HudGuiScale(int factor) {
        this.factor = factor;
    }

    /** @return the fixed GUI scale to emulate, or 0 for {@link #AUTO} */
    public int factor() {
        return factor;
    }
}
