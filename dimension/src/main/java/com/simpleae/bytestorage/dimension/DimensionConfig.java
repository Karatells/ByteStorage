package com.simpleae.bytestorage.dimension;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Общий серверный конфиг аддона Simple Byte Storage: Silicon Plane.
 */
public final class DimensionConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIMENSION;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_SILICON_RIFT_BIOME;
    public static final ForgeConfigSpec.IntValue PORTAL_ACTIVATION_ENERGY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("dimension");
        ENABLE_DIMENSION = b
                .comment("Enable activation of Rift Gates and travel to the Silicon Plane dimension.")
                .define("enableDimension", true);
        REQUIRE_SILICON_RIFT_BIOME = b
                .comment("If true, Rift Gates in the Overworld can ONLY be ignited inside the simple_bytestorage:silicon_rift biome.")
                .define("requireSiliconRiftBiome", true);
        PORTAL_ACTIVATION_ENERGY = b
                .comment("Forge Energy (FE) required inside the Rift Gate Core to ignite a portal.")
                .defineInRange("portalActivationEnergy", 100_000, 0, 10_000_000);
        b.pop();
        SPEC = b.build();
    }

    private DimensionConfig() {
    }
}
