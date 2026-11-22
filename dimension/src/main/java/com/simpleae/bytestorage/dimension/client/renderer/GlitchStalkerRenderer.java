package com.simpleae.bytestorage.dimension.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.GlitchStalkerEntity;
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
 * 3D-рендерер Глитч-Сталкера: высокий фиолетово-чёрный цифровой силуэт с дрожащей глитч-анимацией.
 */
public class GlitchStalkerRenderer extends MobRenderer<GlitchStalkerEntity, GlitchStalkerRenderer.StalkerModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(SimpleAeDimension.MOD_ID, "glitch_stalker"), "main");
    private static final ResourceLocation TEX =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/entity/glitch_stalker.png");

    public GlitchStalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StalkerModel(ctx.bakeLayer(LAYER)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(GlitchStalkerEntity entity) {
        return TEX;
    }

    public static class StalkerModel extends EntityModel<GlitchStalkerEntity> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart leftArm;
        private final ModelPart rightArm;
        private final ModelPart leftLeg;
        private final ModelPart rightLeg;

        public StalkerModel(ModelPart root) {
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.leftArm = root.getChild("left_arm");
            this.rightArm = root.getChild("right_arm");
            this.leftLeg = root.getChild("left_leg");
            this.rightLeg = root.getChild("right_leg");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -4.0F, 0.0F));
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 14.0F, 4.0F), PartPose.offset(0.0F, -4.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 16).addBox(0.0F, -1.0F, -1.5F, 3.0F, 15.0F, 3.0F), PartPose.offset(4.0F, -3.0F, 0.0F));
            root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 15.0F, 3.0F), PartPose.offset(-4.0F, -3.0F, 0.0F));
            root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offset(2.0F, 10.0F, 0.0F));
            root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offset(-2.0F, 10.0F, 0.0F));
            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(GlitchStalkerEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
            float walk = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
            this.leftLeg.xRot = walk;
            this.rightLeg.xRot = -walk;
            this.leftArm.xRot = -walk * 0.7F;
            this.rightArm.xRot = walk * 0.7F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            head.render(poseStack, buffer, light, overlay, r, g, b, a);
            body.render(poseStack, buffer, light, overlay, r, g, b, a);
            leftArm.render(poseStack, buffer, light, overlay, r, g, b, a);
            rightArm.render(poseStack, buffer, light, overlay, r, g, b, a);
            leftLeg.render(poseStack, buffer, light, overlay, r, g, b, a);
            rightLeg.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }
}
