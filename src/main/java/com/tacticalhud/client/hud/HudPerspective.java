package com.tacticalhud.client.hud;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Temporarily swaps the orthographic GUI projection for a perspective one, so ordinary {@link GuiGraphics}
 * draw calls land on a panel that is really rotated in 3D, the way The Division's HUD floats next to the agent.
 * <p>
 * The camera distance is chosen so that, on the panel's pivot plane, one local unit is exactly one
 * GUI-scaled pixel; positions and sizes therefore keep their usual GUI meaning.
 * <p>
 * Render thread only. Every {@link #begin} must be paired with {@link #end}.
 */
final class HudPerspective {
    /**
     * Near plane far enough from the camera to keep depth precision well below the layer offsets the
     * painter uses; the panel sits hundreds of units away, so nothing is clipped.
     */
    private static final float NEAR_PLANE = 10f;
    private static final float FAR_PLANE = 5000f;

    private HudPerspective() {
    }

    /**
     * @param pivotScreenX where the panel pivot appears, in GUI pixels
     * @param pivotLocalX  the pivot in panel-local units, which rotation and scale are applied around
     */
    static void begin(GuiGraphics graphics, int guiWidth, int guiHeight, float fovDegrees,
                      float pivotScreenX, float pivotScreenY, float pivotLocalX, float pivotLocalY,
                      float yawDegrees, float tiltDegrees, float scale) {
        graphics.flush();
        float fov = fovDegrees * Mth.DEG_TO_RAD;
        float distance = guiHeight / (2f * (float) Math.tan(fov / 2f));

        RenderSystem.backupProjectionMatrix();
        Matrix4f projection = new Matrix4f().setPerspective(fov, (float) guiWidth / guiHeight, NEAR_PLANE, FAR_PLANE);
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

        // The GUI model-view pushes everything to the far end of the orthographic depth range; drop that.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        // Earlier overlays wrote orthographic depth that would wrongly occlude the panel.
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.setIdentity();
        pose.translate(pivotScreenX - guiWidth / 2f, guiHeight / 2f - pivotScreenY, -distance);
        pose.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        pose.mulPose(Axis.XP.rotationDegrees(tiltDegrees));
        // Flip Y so GUI-style "down is positive" coordinates keep working. The orthographic GUI projection
        // flips Y as well, so triangle winding (and therefore back-face culling) is unchanged.
        pose.scale(scale, -scale, scale);
        pose.translate(-pivotLocalX, -pivotLocalY, 0);
    }

    static void end(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().popPose();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
