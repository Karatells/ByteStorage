package com.simpleae.bytestorage.dimension.client.renderer;

import com.mojang.datafixers.util.Pair;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

/**
 * Рендерер оптической лодки и лодки с сундуком (использует textures/entity/boat/optic.png и chest_boat/optic.png).
 */
public class OpticBoatRenderer extends BoatRenderer {

    private final Pair<ResourceLocation, ListModel<Boat>> opticModel;

    public OpticBoatRenderer(EntityRendererProvider.Context ctx, boolean hasChest) {
        super(ctx, hasChest);
        ResourceLocation tex = new ResourceLocation(SimpleAeDimension.MOD_ID,
                hasChest ? "textures/entity/chest_boat/optic.png" : "textures/entity/boat/optic.png");
        ListModel<Boat> model = hasChest
                ? new ChestBoatModel(ctx.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.OAK)))
                : new BoatModel(ctx.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
        this.opticModel = Pair.of(tex, model);
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return opticModel;
    }
}
