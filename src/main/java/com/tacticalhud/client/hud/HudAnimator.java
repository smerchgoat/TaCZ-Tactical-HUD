package com.tacticalhud.client.hud;

import com.tacz.guns.api.entity.ReloadState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * Purely visual, frame-rate independent HUD state: fade, boot-up sweep, camera sway, health ghost bar,
 * shot flash, damage shake and reload progress. Never feeds anything back into gameplay.
 */
final class HudAnimator {
    /** Frames longer than this (pause, alt-tab) are clamped so animations don't jump. */
    private static final float MAX_FRAME_SECONDS = 0.1f;
    private static final float FADE_PER_SECOND = 5f;
    private static final float BOOT_SECONDS = 0.45f;
    private static final float HEALTH_FOLLOW_RATE = 14f;
    private static final float GHOST_HOLD_SECONDS = 0.5f;
    private static final float GHOST_DRAIN_PER_SECOND = 0.6f;
    private static final float SHOT_FLASH_DECAY_PER_SECOND = 7f;
    private static final float SHAKE_DECAY_PER_SECOND = 3.5f;
    private static final float SWAY_RETURN_RATE = 7f;
    /**
     * Second smoothing stage on top of the raw camera-lag offset. Mouse input arrives in uneven per-frame
     * steps, and following them directly makes the panel tremble during fast turns.
     */
    private static final float SWAY_SMOOTH_RATE = 12f;
    private static final float SWAY_YAW_FACTOR = 0.35f;
    private static final float SWAY_PITCH_FACTOR = 0.25f;
    private static final float SWAY_LIMIT = 14f;
    private static final float VIEW_BLEND_PER_SECOND = 4f;

    private long lastFrameNanos = -1L;
    private float time;
    private float alpha;
    private float boot;
    private float shownHealth = 1f;
    private float ghostHealth = 1f;
    private float ghostHoldSeconds;
    private float lastHealthTarget = 1f;
    private float shotFlash;
    private float damageShake;
    private float swayTargetX;
    private float swayTargetY;
    private float swayX;
    private float swayY;
    private float viewBlend;
    private float lastYaw;
    private float lastPitch;
    private boolean hasLastView;
    private ResourceLocation lastGunId;
    private int lastMagAmmo = -1;
    private ReloadState.StateType lastReloadType = ReloadState.StateType.NOT_RELOADING;
    private long reloadPhaseTotal = 1L;
    private float reloadProgress = -1f;

    /**
     * @param snapshot   data of the gun in hand, or null while fading out after the gun was put away
     * @param viewTarget 1 for the first person layout, 0 for third person
     */
    void update(LocalPlayer player, HudSnapshot snapshot, float partialTick, float swayStrength, float viewTarget) {
        float dt = advanceClock();
        time += dt;
        boolean visible = snapshot != null;
        if (visible) {
            // Glide between the first and third person layouts on F5, but appear directly in place.
            viewBlend = alpha > 0f ? approach(viewBlend, viewTarget, VIEW_BLEND_PER_SECOND * dt) : viewTarget;
        }
        alpha = approach(alpha, visible ? 1f : 0f, FADE_PER_SECOND * dt);
        if (!visible) {
            if (alpha <= 0f) {
                boot = 0f;
                lastGunId = null;
                hasLastView = false;
                swayTargetX = swayTargetY = swayX = swayY = 0f;
            }
            return;
        }
        boot = Math.min(1f, boot + dt / BOOT_SECONDS);
        updateSway(player, partialTick, swayStrength, dt);
        updateHealth(snapshot.healthFraction(), dt);
        updateAmmo(snapshot, dt);
        updateReload(snapshot);
    }

    private float advanceClock() {
        long now = System.nanoTime();
        float dt = lastFrameNanos < 0 ? 0f : (now - lastFrameNanos) / 1.0e9f;
        lastFrameNanos = now;
        return Mth.clamp(dt, 0f, MAX_FRAME_SECONDS);
    }

    private void updateSway(LocalPlayer player, float partialTick, float strength, float dt) {
        float yaw = player.getViewYRot(partialTick);
        float pitch = player.getViewXRot(partialTick);
        if (hasLastView) {
            float decay = (float) Math.exp(-dt * SWAY_RETURN_RATE);
            // Turning right moves the world left on screen; the HUD lags with the world.
            swayTargetX = softLimit(swayTargetX * decay - Mth.wrapDegrees(yaw - lastYaw) * SWAY_YAW_FACTOR * strength);
            swayTargetY = softLimit(swayTargetY * decay - (pitch - lastPitch) * SWAY_PITCH_FACTOR * strength);
        }
        float follow = 1f - (float) Math.exp(-dt * SWAY_SMOOTH_RATE);
        swayX += (swayTargetX - swayX) * follow;
        swayY += (swayTargetY - swayY) * follow;
        lastYaw = yaw;
        lastPitch = pitch;
        hasLastView = true;
    }

    private void updateHealth(float target, float dt) {
        if (target < lastHealthTarget - 1.0e-3f) {
            ghostHoldSeconds = GHOST_HOLD_SECONDS;
            damageShake = 1f;
        }
        lastHealthTarget = target;
        shownHealth += (target - shownHealth) * (1f - (float) Math.exp(-dt * HEALTH_FOLLOW_RATE));
        if (target >= ghostHealth) {
            ghostHealth = target;
        } else if (ghostHoldSeconds > 0f) {
            ghostHoldSeconds -= dt;
        } else {
            ghostHealth = Math.max(target, ghostHealth - GHOST_DRAIN_PER_SECOND * dt);
        }
        damageShake = Math.max(0f, damageShake - SHAKE_DECAY_PER_SECOND * dt);
    }

    private void updateAmmo(HudSnapshot snapshot, float dt) {
        boolean sameGun = Objects.equals(lastGunId, snapshot.gunId());
        if (sameGun && snapshot.magAmmo() < lastMagAmmo) {
            shotFlash = 1f;
        }
        if (!sameGun) {
            // Re-run the boot sweep when switching weapons, as the panel content changes entirely.
            boot = Math.min(boot, 0.35f);
        }
        lastGunId = snapshot.gunId();
        lastMagAmmo = snapshot.magAmmo();
        shotFlash = Math.max(0f, shotFlash - SHOT_FLASH_DECAY_PER_SECOND * dt);
    }

    private void updateReload(HudSnapshot snapshot) {
        ReloadState.StateType type = snapshot.reloadStateType();
        if (!type.isReloading()) {
            reloadProgress = -1f;
        } else {
            // The synced countdown is per phase, so remember each phase's starting value as its length.
            if (type != lastReloadType) {
                reloadPhaseTotal = Math.max(snapshot.reloadCountDown(), 1L);
            }
            reloadProgress = type.isReloadFinishing() ? 1f
                    : Mth.clamp(1f - (float) snapshot.reloadCountDown() / reloadPhaseTotal, 0f, 1f);
        }
        lastReloadType = type;
    }

    /** Saturates smoothly toward the limit; a hard clamp makes the panel judder while pinned against it. */
    private static float softLimit(float value) {
        return SWAY_LIMIT * (float) Math.tanh(value / SWAY_LIMIT);
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** Eased 0..1 blend from the third person layout (0) to the first person layout (1). */
    float viewBlend() {
        return viewBlend * viewBlend * (3f - 2f * viewBlend);
    }

    float alpha() {
        return alpha;
    }

    float boot() {
        return boot;
    }

    float time() {
        return time;
    }

    float shownHealth() {
        return shownHealth;
    }

    float ghostHealth() {
        return ghostHealth;
    }

    float shotFlash() {
        return shotFlash;
    }

    float reloadProgress() {
        return reloadProgress;
    }

    float offsetX() {
        return swayX + Mth.sin(time * 55f) * damageShake * 2.5f;
    }

    float offsetY() {
        return swayY + Mth.cos(time * 47f) * damageShake * 1.5f;
    }
}
