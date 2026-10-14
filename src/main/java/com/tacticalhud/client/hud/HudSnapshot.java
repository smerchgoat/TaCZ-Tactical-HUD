package com.tacticalhud.client.hud;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Immutable per-frame view of everything the 3D HUD displays. Built by {@link HudSnapshotFactory}, so the
 * painter never touches the player, the inventory or TaCZ item APIs directly.
 *
 * @param heat           heat fraction in [0, 1], or {@link #NO_HEAT} if the gun has no heat mechanic
 * @param reserveText    reserve ammo, empty for guns fed straight from the inventory
 * @param otherGunAmmo   magazine counts of up to {@link #OTHER_GUN_SLOTS} other guns in the hotbar, null for an empty slot
 */
public record HudSnapshot(
        ResourceLocation gunId,
        int magAmmo,
        int magCapacity,
        String reserveText,
        String spareMagsText,
        boolean overheatLocked,
        float heat,
        FireMode fireMode,
        ResourceLocation gunHudTexture,
        ReloadState.StateType reloadStateType,
        long reloadCountDown,
        String[] otherGunAmmo,
        int grenades,
        int medkits,
        float healthFraction,
        int armorValue,
        float aimingProgress
) {
    public static final float NO_HEAT = -1f;
    public static final int OTHER_GUN_SLOTS = 2;

    public boolean hasHeat() {
        return heat >= 0;
    }

    public boolean isLowAmmo() {
        return magAmmo <= magCapacity / 4;
    }

    @Nullable
    public String otherGunAmmo(int slot) {
        return otherGunAmmo[slot];
    }
}
