package com.simpleae.bytestorage.dimension.fluid;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Жидкий хладагент (Liquid Coolant) — использует родные анимированные текстуры из обновлённого ассет-пака.
 */
public class CoolantFluidType extends FluidType {

    private static final ResourceLocation STILL =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "block/liquid_coolant_still");
    private static final ResourceLocation FLOW =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "block/liquid_coolant_flow");
    private static final ResourceLocation OVERLAY =
            new ResourceLocation("minecraft", "block/water_overlay");

    public CoolantFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF; // Используем родной цвет анимированной текстуры
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                           int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.10F, 0.78F, 0.90F);
            }
        });
    }
}
