package com.simpleae.bytestorage.dimension.client.renderer;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.GlitchStalkerEntity;
import net.minecraft.client.model.HierarchicalModel;
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
 * 3D-рендерер Глитч-Сталкера на базе обновлённой модели и UV-текстуры из ассет-пака (с парящими глитч-осколками).
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

    public static class StalkerModel extends HierarchicalModel<GlitchStalkerEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart armRight;
        private final ModelPart armLeft;
        private final ModelPart legRight;
        private final ModelPart legLeft;
        private final ModelPart glitchShards;

        public StalkerModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.armRight = root.getChild("arm_right");
            this.armLeft = root.getChild("arm_left");
            this.legRight = root.getChild("leg_right");
            this.legLeft = root.getChild("leg_left");
            this.glitchShards = root.getChild("glitch_shards");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-3.00F, -6.00F, -3.00F, 6.00F, 7.00F, 6.00F),
                    PartPose.offsetAndRotation(0.00F, -2.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("upper_torso", CubeListBuilder.create()
                    .texOffs(25, 0).addBox(-4.00F, -4.00F, -2.00F, 8.00F, 7.00F, 4.00F)
                    .texOffs(21, 26).addBox(-2.00F, 4.00F, -1.50F, 4.00F, 3.00F, 3.00F),
                    PartPose.offsetAndRotation(0.00F, 4.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("lower_torso", CubeListBuilder.create()
                    .texOffs(0, 26).addBox(-3.00F, -1.00F, -2.00F, 6.00F, 4.00F, 4.00F),
                    PartPose.offsetAndRotation(0.00F, 12.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("arm_right", CubeListBuilder.create()
                    .texOffs(13, 14).addBox(-1.50F, -1.00F, -1.50F, 3.00F, 7.00F, 3.00F)
                    .texOffs(39, 14).addBox(-2.00F, 8.00F, -1.50F, 3.00F, 6.00F, 3.00F),
                    PartPose.offsetAndRotation(-5.50F, 2.00F, 0.00F, -0.2094F, 0.0F, 0.1396F));
            root.addOrReplaceChild("arm_left", CubeListBuilder.create()
                    .texOffs(26, 14).addBox(-1.50F, -1.00F, -1.50F, 3.00F, 7.00F, 3.00F)
                    .texOffs(52, 14).addBox(-1.00F, 8.00F, -1.50F, 3.00F, 6.00F, 3.00F),
                    PartPose.offsetAndRotation(5.50F, 2.00F, 0.00F, -0.2094F, 0.0F, -0.1396F));
            root.addOrReplaceChild("leg_right", CubeListBuilder.create()
                    .texOffs(50, 0).addBox(-1.50F, 0.00F, -1.50F, 3.00F, 8.00F, 3.00F),
                    PartPose.offsetAndRotation(-2.00F, 15.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leg_left", CubeListBuilder.create()
                    .texOffs(0, 14).addBox(-1.50F, 0.00F, -1.50F, 3.00F, 8.00F, 3.00F),
                    PartPose.offsetAndRotation(2.00F, 15.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("glitch_shards", CubeListBuilder.create()
                    .texOffs(36, 26).addBox(-7.00F, -4.00F, 0.00F, 2.00F, 2.00F, 2.00F)
                    .texOffs(45, 26).addBox(6.00F, -3.00F, -4.00F, 2.00F, 2.00F, 2.00F),
                    PartPose.offsetAndRotation(0.00F, 1.00F, 2.00F, 0.0F, 0.4363F, 0.0F));
            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public ModelPart root() {
            return this.root;
        }

        @Override
        public void setupAnim(GlitchStalkerEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
            float walk = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
            this.legLeft.xRot = walk;
            this.legRight.xRot = -walk;
            this.armLeft.xRot = -0.2094F + walk * 0.7F;
            this.armRight.xRot = -0.2094F - walk * 0.7F;
            this.glitchShards.yRot = ageInTicks * 0.09F;
            this.glitchShards.y = 1.0F + Mth.sin(ageInTicks * 0.18F) * 1.2F;
        }
    }
}
