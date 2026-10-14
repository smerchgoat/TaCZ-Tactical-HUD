package com.tacticalhud.client.hud;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacticalhud.config.HudConfig;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nullable;

/**
 * Angled 3D gun HUD in the style of Tom Clancy's The Division, floating beside the character.
 * Replaces TaCZ's own gun HUD whenever {@link HudVisibility} says so (see {@link HudElementHider}).
 */
public class TacticalHudOverlay implements IGuiOverlay {
    /** Panel pivot in local units: the visual center of the content, so rotation keeps it in place. */
    private static final float PIVOT_X = (HudPainter.CONTENT_LEFT + HudPainter.PANEL_WIDTH) / 2f;
    private static final float PIVOT_Y = HudPainter.CONTENT_BOTTOM / 2f;
    /** While aiming the HUD steps back a little, keeping the sight picture clear. */
    private static final float AIM_ALPHA_DROP = 0.3f;
    private static final float AIM_SCALE_DROP = 0.08f;

    private final HudAnimator animator = new HudAnimator();
    private final HudPainter painter = new HudPainter();
    private final AmmoCache ammoCache = new AmmoCache();
    /** Last shown data, kept so the panel can fade out after the gun is put away. */
    @Nullable
    private HudSnapshot lastSnapshot;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        double windowGuiScale = mc.getWindow().getGuiScale();
        HudLayout firstPerson = HudLayout.of(HudConfig.FIRST_PERSON, height, windowGuiScale);
        HudLayout thirdPerson = HudLayout.of(HudConfig.THIRD_PERSON, height, windowGuiScale);
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        HudSnapshot snapshot = HudVisibility.shouldRender(mc) ? capture(player, partialTick) : null;
        float swayStrength = (isFirstPerson ? firstPerson : thirdPerson).swayStrength();
        animator.update(player, snapshot, partialTick, swayStrength, isFirstPerson ? 1f : 0f);
        if (snapshot != null) {
            lastSnapshot = snapshot;
        }
        // Fade out only when the player stops holding a gun, not when switching to the classic HUD.
        boolean fadingOut = snapshot == null && HudVisibility.isEnabledForCamera(mc);
        if (lastSnapshot == null || animator.alpha() <= 0f || (snapshot == null && !fadingOut)) {
            return;
        }
        draw(graphics, width, height, lastSnapshot, HudLayout.lerp(thirdPerson, firstPerson, animator.viewBlend()));
    }

    @Nullable
    private HudSnapshot capture(LocalPlayer player, float partialTick) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return null;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).map(ClientGunIndex::getGunData).orElse(null);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gunData == null || display == null) {
            return null;
        }
        return HudSnapshotFactory.capture(player, stack, iGun, gunData, display, ammoCache, partialTick);
    }

    private void draw(GuiGraphics graphics, int width, int height, HudSnapshot snapshot, HudLayout layout) {
        float aim = snapshot.aimingProgress();
        float alpha = animator.alpha() * (1f - AIM_ALPHA_DROP * aim);
        float scale = layout.scale() * (1f - AIM_SCALE_DROP * aim);
        float pivotX = width * layout.anchorX() + animator.offsetX();
        float pivotY = height * layout.anchorY() + animator.offsetY();

        HudPerspective.begin(graphics, width, height, layout.fov(),
                pivotX, pivotY, PIVOT_X, PIVOT_Y, layout.yaw(), layout.tilt(), scale);
        try {
            painter.paint(graphics, Minecraft.getInstance().font, snapshot, animator, alpha, layout.backgroundOpacity());
        } finally {
            // Always restore the GUI projection, or every later overlay and screen renders broken.
            HudPerspective.end(graphics);
        }
    }
}
