package com.simpleae.bytestorage.dimension.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Кастомный Skybox измерения «Кремниевый План» (Silicon Plane):
 * - Биомный тёмно-индиговый градиент неба
 * - Звёздный купол (silicon_sky_stars.png)
 * - Полупрозрачная бирюзовая матричная сетка (silicon_matrix_grid.png)
 * - Кольцо полярного сияния по всему горизонту (silicon_aurora.png) с плавным дрейфом
 */
public class SiliconPlaneSkyEffects extends DimensionSpecialEffects {

    private static final ResourceLocation TEX_STARS =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/environment/silicon_sky_stars.png");
    private static final ResourceLocation TEX_GRID =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/environment/silicon_matrix_grid.png");
    private static final ResourceLocation TEX_AURORA =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/environment/silicon_aurora.png");

    public SiliconPlaneSkyEffects() {
        super(196.0F, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        return biomeFogColor.multiply(0.85, 0.92, 1.0);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        float time = ticks + partialTick;

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        // 1. Звёздный купол (6 граней) с мягкой биомной тонировкой
        Vec3 skyCol = level.getSkyColor(camera.getPosition(), partialTick);
        int sr = Math.min(255, (int) (skyCol.x * 90) + 18);
        int sg = Math.min(255, (int) (skyCol.y * 105) + 26);
        int sb = Math.min(255, (int) (skyCol.z * 140) + 48);

        // Заливаем фон неба (тёмный индиго-биомный куб)
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.defaultBlendFunc();
        drawColorCube(poseStack, sr, sg, sb, 255);

        // 2. Аддитивный слой звёзд (silicon_sky_stars.png)
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        RenderSystem.setShaderTexture(0, TEX_STARS);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.015F));
        drawTexturedCube(poseStack, 1.5F, 210, 235, 255, 200);
        poseStack.popPose();

        // 3. Полупрозрачная бирюзовая матричная сетка (silicon_matrix_grid.png)
        RenderSystem.setShaderTexture(0, TEX_GRID);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.008F));
        drawTexturedCube(poseStack, 3.0F, 64, 230, 255, 72);
        poseStack.popPose();

        // 4. Кольцо полярного сияния (silicon_aurora.png) на высоте 20..58 градусов по всему небу!
        RenderSystem.setShaderTexture(0, TEX_AURORA);
        drawAuroraRing(poseStack, time);

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        return true;
    }

    private static void drawColorCube(PoseStack poseStack, int r, int g, int b, int a) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        for (int face = 0; face < 6; face++) {
            poseStack.pushPose();
            orientFace(poseStack, face);
            Matrix4f mat = poseStack.last().pose();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(mat, -100.0F, -100.0F, -100.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, -100.0F, -100.0F, 100.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, 100.0F, -100.0F, 100.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, 100.0F, -100.0F, -100.0F).color(r, g, b, a).endVertex();
            tess.end();
            poseStack.popPose();
        }
    }

    private static void drawTexturedCube(PoseStack poseStack, float uvScale, int r, int g, int b, int a) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        for (int face = 0; face < 6; face++) {
            poseStack.pushPose();
            orientFace(poseStack, face);
            Matrix4f mat = poseStack.last().pose();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buf.vertex(mat, -95.0F, -95.0F, -95.0F).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, -95.0F, -95.0F, 95.0F).uv(0.0F, uvScale).color(r, g, b, a).endVertex();
            buf.vertex(mat, 95.0F, -95.0F, 95.0F).uv(uvScale, uvScale).color(r, g, b, a).endVertex();
            buf.vertex(mat, 95.0F, -95.0F, -95.0F).uv(uvScale, 0.0F).color(r, g, b, a).endVertex();
            tess.end();
            poseStack.popPose();
        }
    }

    private static void drawAuroraRing(PoseStack poseStack, float time) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f mat = poseStack.last().pose();
        int segs = 24;
        float radius = 85.0F;
        float yLow = 14.0F;
        float yHigh = 56.0F;
        float uShift = (time * 0.0006F) % 1.0F;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < segs; i++) {
            double a0 = (2.0 * Math.PI * i) / segs;
            double a1 = (2.0 * Math.PI * (i + 1)) / segs;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;

            float u0 = ((float) i / segs) * 2.0F + uShift;
            float u1 = ((float) (i + 1) / segs) * 2.0F + uShift;

            buf.vertex(mat, x0, yLow, z0).uv(u0, 1.0F).color(120, 235, 255, 195).endVertex();
            buf.vertex(mat, x1, yLow, z1).uv(u1, 1.0F).color(120, 235, 255, 195).endVertex();
            buf.vertex(mat, x1, yHigh, z1).uv(u1, 0.0F).color(185, 130, 255, 175).endVertex();
            buf.vertex(mat, x0, yHigh, z0).uv(u0, 0.0F).color(185, 130, 255, 175).endVertex();
        }
        tess.end();
    }

    private static void orientFace(PoseStack poseStack, int face) {
        if (face == 1) poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        else if (face == 2) poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        else if (face == 3) poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        else if (face == 4) poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        else if (face == 5) poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
    }
}
