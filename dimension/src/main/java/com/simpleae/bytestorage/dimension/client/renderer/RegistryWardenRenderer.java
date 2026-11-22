package com.simpleae.bytestorage.dimension.client.renderer;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.RegistryWardenEntity;
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
 * 3D-рендерер мини-босса Хранителя Реестра с вращающимся золотым нимбом и парящими щитами.
 */
public class RegistryWardenRenderer extends MobRenderer<RegistryWardenEntity, RegistryWardenRenderer.WardenModel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(SimpleAeDimension.MOD_ID, "registry_warden"), "main");
    private static final ResourceLocation TEX =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "textures/entity/registry_warden.png");

    public RegistryWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WardenModel(ctx.bakeLayer(LAYER)), 0.85F);
    }

    @Override
    public ResourceLocation getTextureLocation(RegistryWardenEntity entity) {
        return TEX;
    }

    public static class WardenModel extends HierarchicalModel<RegistryWardenEntity> {
        private final ModelPart root;
        private final ModelPart head;
        private final ModelPart halo;
        private final ModelPart armRight;
        private final ModelPart armLeft;
        private final ModelPart shieldRight;
        private final ModelPart shieldLeft;

        public WardenModel(ModelPart root) {
            this.root = root;
            this.head = root.getChild("head");
            this.halo = root.getChild("halo");
            this.armRight = root.getChild("arm_right");
            this.armLeft = root.getChild("arm_left");
            this.shieldRight = root.getChild("shield_right");
            this.shieldLeft = root.getChild("shield_left");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("torso", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-6.00F, -6.00F, -4.00F, 12.00F, 13.00F, 8.00F)
                    .texOffs(0, 22).addBox(-4.00F, 7.00F, -3.00F, 8.00F, 9.00F, 6.00F),
                    PartPose.offsetAndRotation(0.00F, 2.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("head", CubeListBuilder.create()
                    .texOffs(29, 22).addBox(-3.50F, -8.00F, -3.50F, 7.00F, 8.00F, 7.00F)
                    .texOffs(76, 22).addBox(3.00F, -10.00F, -1.00F, 2.00F, 5.00F, 2.00F)
                    .texOffs(85, 22).addBox(-5.00F, -10.00F, -1.00F, 2.00F, 5.00F, 2.00F),
                    PartPose.offsetAndRotation(0.00F, -5.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("halo", CubeListBuilder.create()
                    .texOffs(94, 22).addBox(-7.00F, -7.00F, -1.00F, 14.00F, 2.00F, 2.00F)
                    .texOffs(0, 38).addBox(-7.00F, 7.00F, -1.00F, 14.00F, 2.00F, 2.00F)
                    .texOffs(58, 22).addBox(6.00F, -5.00F, -1.00F, 2.00F, 12.00F, 2.00F)
                    .texOffs(67, 22).addBox(-8.00F, -5.00F, -1.00F, 2.00F, 12.00F, 2.00F),
                    PartPose.offsetAndRotation(0.00F, -7.00F, 6.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("arm_right", CubeListBuilder.create()
                    .texOffs(75, 0).addBox(-2.00F, -1.00F, -2.00F, 4.00F, 13.00F, 4.00F),
                    PartPose.offsetAndRotation(-8.50F, -2.00F, 0.00F, 0.0F, 0.0F, 0.1047F));
            root.addOrReplaceChild("arm_left", CubeListBuilder.create()
                    .texOffs(92, 0).addBox(-2.00F, -1.00F, -2.00F, 4.00F, 13.00F, 4.00F),
                    PartPose.offsetAndRotation(8.50F, -2.00F, 0.00F, 0.0F, 0.0F, -0.1047F));
            root.addOrReplaceChild("shield_right", CubeListBuilder.create()
                    .texOffs(41, 0).addBox(-1.00F, -7.00F, -3.00F, 2.00F, 15.00F, 6.00F),
                    PartPose.offsetAndRotation(-15.00F, 3.00F, -2.00F, 0.0F, 0.3491F, 0.0F));
            root.addOrReplaceChild("shield_left", CubeListBuilder.create()
                    .texOffs(58, 0).addBox(-1.00F, -7.00F, -3.00F, 2.00F, 15.00F, 6.00F),
                    PartPose.offsetAndRotation(15.00F, 3.00F, -2.00F, 0.0F, -0.3491F, 0.0F));
            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public ModelPart root() {
            return this.root;
        }

        @Override
        public void setupAnim(RegistryWardenEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
            this.halo.zRot = ageInTicks * 0.06F;
            float bob = Mth.sin(ageInTicks * 0.12F) * 1.8F;
            this.shieldRight.y = 3.0F + bob;
            this.shieldLeft.y = 3.0F - bob;
            float swing = Mth.cos(limbSwing * 0.6F) * 0.6F * limbSwingAmount;
            this.armLeft.xRot = swing;
            this.armRight.xRot = -swing;
        }
    }
}
