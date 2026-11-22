package com.simpleae.bytestorage.dimension.world;

import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;

/**
 * Ключи измерения «Кремниевый План» (Silicon Plane) и всех его 8 уникальных биомов.
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

    /** 8 уникальных биомов Кремниевого Плана. */
    public static final ResourceKey<Biome> BIOME_SILICON_TERRACES = biome("silicon_terraces");
    public static final ResourceKey<Biome> BIOME_OPTIC_SPIREWOOD = biome("optic_spirewood");
    public static final ResourceKey<Biome> BIOME_BUS_CANYONS = biome("bus_canyons");
    public static final ResourceKey<Biome> BIOME_CRYO_CACHE = biome("cryo_cache");
    public static final ResourceKey<Biome> BIOME_CAPACITOR_FLATS = biome("capacitor_flats");
    public static final ResourceKey<Biome> BIOME_CORRUPTED_BADLANDS = biome("corrupted_badlands");
    public static final ResourceKey<Biome> BIOME_CLOUD_ARCHIVE = biome("cloud_archive");
    public static final ResourceKey<Biome> BIOME_NULL_CORE = biome("null_core");

    public static final List<ResourceKey<Biome>> ALL_BIOMES = List.of(
            BIOME_SILICON_TERRACES,
            BIOME_OPTIC_SPIREWOOD,
            BIOME_BUS_CANYONS,
            BIOME_CRYO_CACHE,
            BIOME_CAPACITOR_FLATS,
            BIOME_CORRUPTED_BADLANDS,
            BIOME_CLOUD_ARCHIVE,
            BIOME_NULL_CORE
    );

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(SimpleAeDimension.MOD_ID, path));
    }

    public static String translationKey(ResourceKey<Biome> key) {
        ResourceLocation id = key.location();
        return "biome." + id.getNamespace() + "." + id.getPath();
    }

    public static boolean isSiliconPlane(Level level) {
        return level.dimension().equals(SILICON_PLANE_LEVEL);
    }

    public static boolean isInSiliconRiftBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(OVERWORLD_SILICON_RIFT);
    }

    private SiliconPlaneDimension() {
    }
}
