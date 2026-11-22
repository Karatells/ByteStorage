package com.simpleae.bytestorage.dimension.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Сумеречно-киберпанковая атмосфера измерения «Кремниевый План» (Silicon Plane):
 * использует SkyType.NORMAL с высокими облаками, чтобы каждый из 8 биомов
 * отображал свой уникальный цвет неба и светящегося тумана.
 */
public class SiliconPlaneSkyEffects extends DimensionSpecialEffects {

    public SiliconPlaneSkyEffects() {
        super(156.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 biomeFogColor, float daylight) {
        // Сохраняем насыщенный биомный цвет тумана даже в сумерках
        return biomeFogColor.multiply(0.88, 0.94, 1.0);
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
