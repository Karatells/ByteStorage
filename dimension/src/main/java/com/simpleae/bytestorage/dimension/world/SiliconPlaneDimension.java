package com.simpleae.bytestorage.dimension.world;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Ключи измерения «Кремниевый План» (Silicon Plane) и его биомов.
 */
public final class SiliconPlaneDimension {

    public static final ResourceLocation SILICON_PLANE_ID =
            new ResourceLocation(SimpleAeDimension.MOD_ID, "silicon_plane");

    public static final ResourceKey<Level> SILICON_PLANE_LEVEL =
            ResourceKey.create(Registries.DIMENSION, SILICON_PLANE_ID);

    public static final ResourceKey<DimensionType> SILICON_PLANE_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, SILICON_PLANE_ID);

    /** Биом из основного мода Simple Byte Storage, в котором только и могут работать Врата Разлома. */
    public static final ResourceKey<Biome> OVERWORLD_SILICON_RIFT =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("simple_bytestorage", "silicon_rift"));

    /** 3 стартовых биома Этапа 1 (MVP). */
    public static final ResourceKey<Biome> BIOME_SILICON_TERRACES =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(SimpleAeDimension.MOD_ID, "silicon_terraces"));
    public static final ResourceKey<Biome> BIOME_OPTIC_SPIREWOOD =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(SimpleAeDimension.MOD_ID, "optic_spirewood"));
    public static final ResourceKey<Biome> BIOME_BUS_CANYONS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation(SimpleAeDimension.MOD_ID, "bus_canyons"));

    public static boolean isSiliconPlane(Level level) {
        return level.dimension().equals(SILICON_PLANE_LEVEL);
    }

    public static boolean isInSiliconRiftBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(OVERWORLD_SILICON_RIFT);
    }

    private SiliconPlaneDimension() {
    }
}
