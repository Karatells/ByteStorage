package com.simpleae.bytestorage.dimension.client.renderer;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import com.simpleae.bytestorage.dimension.entity.ByteSentinelEntity;
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
 * 3D-рендерер Байт-Стража на базе обновлённой модели и UV-текстуры из ассет-пака.
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

    public static class SentinelModel extends HierarchicalModel<ByteSentinelEntity> {
        private final ModelPart root;
        private final ModelPart coreBody;
        private final ModelPart finRight;
        private final ModelPart finLeft;
        private final ModelPart finTopRight;
        private final ModelPart finTopLeft;

        public SentinelModel(ModelPart root) {
            this.root = root;
            this.coreBody = root.getChild("core_body");
            this.finRight = root.getChild("fin_right");
            this.finLeft = root.getChild("fin_left");
            this.finTopRight = root.getChild("fin_top_right");
            this.finTopLeft = root.getChild("fin_top_left");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("core_body", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-4.00F, -4.00F, -4.00F, 8.00F, 8.00F, 8.00F)
                    .texOffs(0, 26).addBox(-2.00F, -2.00F, -5.00F, 4.00F, 4.00F, 1.00F)
                    .texOffs(0, 17).addBox(-3.00F, -6.00F, -3.00F, 6.00F, 2.00F, 6.00F)
                    .texOffs(43, 17).addBox(-2.00F, 4.00F, -2.00F, 4.00F, 2.00F, 4.00F),
                    PartPose.offsetAndRotation(0.00F, 8.00F, 0.00F, 0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("fin_right", CubeListBuilder.create()
                    .texOffs(33, 0).addBox(-3.00F, -4.00F, -2.00F, 2.00F, 8.00F, 4.00F),
                    PartPose.offsetAndRotation(-6.00F, 8.00F, 0.00F, 0.0F, 0.0F, -0.3491F));
            root.addOrReplaceChild("fin_left", CubeListBuilder.create()
                    .texOffs(46, 0).addBox(1.00F, -4.00F, -2.00F, 2.00F, 8.00F, 4.00F),
                    PartPose.offsetAndRotation(6.00F, 8.00F, 0.00F, 0.0F, 0.0F, 0.3491F));
            root.addOrReplaceChild("fin_top_right", CubeListBuilder.create()
                    .texOffs(25, 17).addBox(-2.00F, -5.00F, -1.00F, 2.00F, 5.00F, 2.00F),
                    PartPose.offsetAndRotation(-4.00F, 3.00F, 1.00F, 0.2618F, 0.0F, -0.6109F));
            root.addOrReplaceChild("fin_top_left", CubeListBuilder.create()
                    .texOffs(34, 17).addBox(0.00F, -5.00F, -1.00F, 2.00F, 5.00F, 2.00F),
                    PartPose.offsetAndRotation(4.00F, 3.00F, 1.00F, 0.2618F, 0.0F, 0.6109F));
            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public ModelPart root() {
            return this.root;
        }

        @Override
        public void setupAnim(ByteSentinelEntity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            float bob = Mth.sin(ageInTicks * 0.14F) * 1.4F;
            this.coreBody.y = 8.0F + bob;
            this.coreBody.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
            this.finRight.y = 8.0F + bob;
            this.finLeft.y = 8.0F + bob;
            this.finTopRight.y = 3.0F + bob;
            this.finTopLeft.y = 3.0F + bob;
            float wing = Mth.sin(ageInTicks * 0.2F) * 0.14F;
            this.finRight.zRot = -0.3491F - wing;
            this.finLeft.zRot = 0.3491F + wing;
        }
    }
}
