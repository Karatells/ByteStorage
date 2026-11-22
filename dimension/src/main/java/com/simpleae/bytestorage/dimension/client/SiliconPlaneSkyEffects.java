package com.simpleae.bytestorage.dimension.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Космически-сумеречная атмосфера измерения «Кремниевый План» (Silicon Plane) с бирюзовым отсветом горизонта.
 */
public class SiliconPlaneSkyEffects extends DimensionSpecialEffects {

    public SiliconPlaneSkyEffects() {
        super(192.0F, true, SkyType.END, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        return biomeFogColor.multiply(0.85, 0.92, 1.0);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }
}
