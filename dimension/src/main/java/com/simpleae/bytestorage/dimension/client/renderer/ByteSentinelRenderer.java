package com.simpleae.bytestorage.dimension.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.ByteSentinelEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 3D-рендерер Байт-Стража: парящий кибер-куб с вращающимися вокруг орбитальными щитками.
 */
public class ByteSentinelRenderer extends MobRenderer<ByteSentinelEntity, ByteSentinelRenderer.SentinelModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(SimpleAeDimension.MOD_ID, "byte_sentinel"), "main");
    private static final ResourceLocation TEX =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/entity/byte_sentinel.png");

    public ByteSentinelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SentinelModel(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ByteSentinelEntity entity) {
        return TEX;
    }

    public static class SentinelModel extends EntityModel<ByteSentinelEntity> {
        private final ModelPart core;
        private final ModelPart ring;

        public SentinelModel(ModelPart root) {
            this.core = root.getChild("core");
            this.ring = root.getChild("ring");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("core",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 6.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                    PartPose.ZERO);
            PartDefinition ring = root.addOrReplaceChild("ring", CubeListBuilder.create(), PartPose.offset(0.0F, 11.0F, 0.0F));
            ring.addOrReplaceChild("plate1", CubeListBuilder.create().texOffs(0, 20).addBox(-9.0F, -3.0F, -2.0F, 2.0F, 6.0F, 4.0F), PartPose.ZERO);
            ring.addOrReplaceChild("plate2", CubeListBuilder.create().texOffs(0, 20).addBox(7.0F, -3.0F, -2.0F, 2.0F, 6.0F, 4.0F), PartPose.ZERO);
            ring.addOrReplaceChild("plate3", CubeListBuilder.create().texOffs(12, 20).addBox(-2.0F, -3.0F, -9.0F, 4.0F, 6.0F, 2.0F), PartPose.ZERO);
            ring.addOrReplaceChild("plate4", CubeListBuilder.create().texOffs(12, 20).addBox(-2.0F, -3.0F, 7.0F, 4.0F, 6.0F, 2.0F), PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        public void setupAnim(ByteSentinelEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            float bob = Mth.sin(ageInTicks * 0.14F) * 1.8F;
            this.core.y = bob;
            this.core.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.ring.y = 11.0F + bob;
            this.ring.yRot = ageInTicks * 0.12F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            core.render(poseStack, buffer, light, overlay, r, g, b, a);
            ring.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }
}
