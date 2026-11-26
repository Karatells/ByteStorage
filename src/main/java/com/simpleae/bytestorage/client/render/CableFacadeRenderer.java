package com.simpleae.bytestorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simpleae.bytestorage.block.entity.CableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Рендерит выбранный блок-фасад поверх Network Cable. */
public class CableFacadeRenderer implements BlockEntityRenderer<CableBlockEntity> {

    public CableFacadeRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(CableBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState facade = be.getFacade();
        if (facade == null || facade.isAir()) {
            return;
        }
        poseStack.pushPose();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                facade, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
