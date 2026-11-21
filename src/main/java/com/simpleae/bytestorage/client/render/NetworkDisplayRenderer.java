package com.simpleae.bytestorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simpleae.bytestorage.block.NetworkDisplayBlock;
import com.simpleae.bytestorage.block.entity.NetworkDisplayBlockEntity;
import com.simpleae.bytestorage.util.DisplayLineParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * 3D-рендер строк и прогресс-баров прямо на передней грани Network Display в мире.
 */
public class NetworkDisplayRenderer implements BlockEntityRenderer<NetworkDisplayBlockEntity> {

    private static final float SCREEN_SCALE = 1.0F / 128.0F;
    private static final int CONTENT_W = 104;
    private static final int LINE_STEP = 14;
    private static final int START_X = -52;
    private static final int START_Y = -42;

    private final Font font;

    public NetworkDisplayRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(NetworkDisplayBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean anyLine = false;
        for (int i = 0; i < NetworkDisplayBlockEntity.MAX_LINES; i++) {
            if (!be.getLine(i).isEmpty()) {
                anyLine = true;
                break;
            }
        }
        if (!anyLine) {
            return;
        }

        BlockState state = be.getBlockState();
        Direction facing = state.hasProperty(NetworkDisplayBlock.FACING)
                ? state.getValue(NetworkDisplayBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingDegrees(facing)));
        poseStack.translate(0.0, 0.0, 0.503);
        poseStack.scale(SCREEN_SCALE, -SCREEN_SCALE, SCREEN_SCALE);

        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < NetworkDisplayBlockEntity.MAX_LINES; i++) {
            String raw = be.getLine(i);
            if (raw.isEmpty()) {
                continue;
            }
            DisplayLineParser.ParsedLine parsed = DisplayLineParser.parse(raw);
            int y = START_Y + i * LINE_STEP;
            if (parsed.isBar()) {
                renderBar(matrix, buffer, START_X, y + 1, CONTENT_W, 8, parsed.barPercent(), parsed.color());
                String pct = parsed.text();
                int tw = this.font.width(pct);
                this.font.drawInBatch(pct, START_X + (CONTENT_W - tw) / 2.0F, y + 1,
                        0xFFFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            } else {
                String clipped = this.font.plainSubstrByWidth(parsed.text(), CONTENT_W);
                this.font.drawInBatch(clipped, START_X, y,
                        parsed.color(), false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
        }

        poseStack.popPose();
    }

    private static void renderBar(Matrix4f matrix, MultiBufferSource buffer,
                                  int x, int y, int w, int h, int percent, int argb) {
        VertexConsumer vc = buffer.getBuffer(RenderType.gui());
        fillRect(vc, matrix, x, y, x + w, y + h, 0xCC0D1824);
        int fillW = Math.round((w - 2) * (percent / 100.0F));
        if (fillW > 0) {
            fillRect(vc, matrix, x + 1, y + 1, x + 1 + fillW, y + h - 1, argb);
        }
    }

    private static void fillRect(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        vc.vertex(m, x1, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, x2, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, x2, y1, 0).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y1, 0).color(r, g, b, a).endVertex();
    }

    private static float facingDegrees(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 180.0F; // NORTH
        };
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}
