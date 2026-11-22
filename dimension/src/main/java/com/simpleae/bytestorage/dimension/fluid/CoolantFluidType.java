package com.simpleae.bytestorage.dimension.fluid;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Жидкий хладагент (Liquid Coolant) — светящаяся голубая техно-жидкость из Магистральных Каньонов.
 */
public class CoolantFluidType extends FluidType {

    private static final ResourceLocation WATER_STILL = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation WATER_FLOW = new ResourceLocation("minecraft", "block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = new ResourceLocation("minecraft", "block/water_overlay");

    public CoolantFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return 0xD820F0FF; // Яркий бирюзово-голубой хладагент
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.08F, 0.72F, 0.85F);
            }
        });
    }
}
