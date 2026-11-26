package com.simpleae.bytestorage.dimension.client.renderer;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.CryoHarvesterEntity;
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
 * 3D-рендерер Крио-Сборщика на базе модели и UV-текстуры из ассет-пака.
 */
public class CryoHarvesterRenderer extends MobRenderer<CryoHarvesterEntity, CryoHarvesterRenderer.HarvesterModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(SimpleAeDimension.MOD_ID, "cryo_harvester"), "main");
    private static final ResourceLocation TEX =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/entity/cryo_harvester.png");

    public CryoHarvesterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HarvesterModel(ctx.bakeLayer(LAYER)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(CryoHarvesterEntity entity) {
        return TEX;
    }

    public static class HarvesterModel extends HierarchicalModel<CryoHarvesterEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart armRight;
        private final ModelPart armLeft;
        private final ModelPart legRight;
        private final ModelPart legLeft;

        public HarvesterModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.armRight = root.getChild("arm_right");
            this.armLeft = root.getChild("arm_left");
            this.legRight = root.getChild("leg_right");
            this.legLeft = root.getChild("leg_left");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("torso", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-6.00F, -7.00F, -4.00F, 12.00F, 12.00F, 8.00F)
                    .texOffs(75, 21).addBox(-1.50F, -11.00F, 3.00F, 3.00F, 6.00F, 3.00F)
                    .texOffs(118, 21).addBox(2.00F, -9.00F, 3.00F, 2.00F, 5.00F, 2.00F)
                    .texOffs(0, 34).addBox(-4.00F, -9.00F, 3.00F, 2.00F, 5.00F, 2.00F),
                    PartPose.offsetAndRotation(0.00F, 9.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(0, 21).addBox(-3.00F, -5.00F, -3.00F, 6.00F, 6.00F, 6.00F),
                    PartPose.offsetAndRotation(0.00F, 2.00F, -2.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("arm_right", CubeListBuilder.create()
                    .texOffs(25, 21).addBox(-3.00F, -4.00F, -3.50F, 5.00F, 5.00F, 7.00F)
                    .texOffs(41, 0).addBox(-2.50F, 1.00F, -2.50F, 4.00F, 11.00F, 5.00F)
                    .texOffs(88, 21).addBox(-2.00F, 12.00F, -2.00F, 3.00F, 4.00F, 4.00F),
                    PartPose.offsetAndRotation(-8.00F, 5.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("arm_left", CubeListBuilder.create()
                    .texOffs(50, 21).addBox(-2.00F, -4.00F, -3.50F, 5.00F, 5.00F, 7.00F)
                    .texOffs(60, 0).addBox(-1.50F, 1.00F, -2.50F, 4.00F, 11.00F, 5.00F)
                    .texOffs(103, 21).addBox(-1.00F, 12.00F, -2.00F, 3.00F, 4.00F, 4.00F),
                    PartPose.offsetAndRotation(8.00F, 5.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leg_right", CubeListBuilder.create()
                    .texOffs(79, 0).addBox(-2.00F, 0.00F, -2.50F, 4.00F, 10.00F, 5.00F),
                    PartPose.offsetAndRotation(-3.50F, 14.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("leg_left", CubeListBuilder.create()
                    .texOffs(98, 0).addBox(-2.00F, 0.00F, -2.50F, 4.00F, 10.00F, 5.00F),
                    PartPose.offsetAndRotation(3.50F, 14.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public ModelPart root() {
            return this.root;
        }

        @Override
        public void setupAnim(CryoHarvesterEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
            float walk = Mth.cos(limbSwing * 0.6F) * 0.85F * limbSwingAmount;
            this.legLeft.xRot = walk;
            this.legRight.xRot = -walk;
            this.armLeft.xRot = -walk * 0.7F;
            this.armRight.xRot = walk * 0.7F;
        }
    }
}
