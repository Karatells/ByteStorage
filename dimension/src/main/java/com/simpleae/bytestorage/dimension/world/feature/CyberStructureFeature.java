package com.simpleae.bytestorage.dimension.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simpleae.bytestorage.dimension.SimpleAeDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Генератор уникальных техногенных и природных структур для каждого из 8 биомов Кремниевого Плана:
 * - server_tower: многоярусная Серверная Башня с LED-панелями, стеклом и маяком
 * - capacitor_pillar: высокий Столб-Конденсатор с проводящими кольцами и энергоядром
 * - cryo_spire: ледяной шестигранный Крио-Шпиль со светящейся сердцевиной
 * - floating_island: парящий кибер-остров с водопадом Жидкого хладагента или шпилем
 * - registry_pylon: обсидианово-золотой Пилон Реестра с аркой и световым ядром
 * - glitch_monolith: парящий искажённый фиолетовый глитч-монолит
 */
public class CyberStructureFeature extends Feature<CyberStructureFeature.Config> {

    public record Config(String kind) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(inst ->
                inst.group(Codec.STRING.fieldOf("kind").forGetter(Config::kind))
                        .apply(inst, Config::new));
    }

    public CyberStructureFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rng = ctx.random();
        String kind = ctx.config().kind();

        if (!level.getBlockState(origin.below()).isSolidRender(level, origin.below())) {
            return false;
        }

        return switch (kind) {
            case "server_tower" -> buildServerTower(level, origin, rng);
            case "capacitor_pillar" -> buildCapacitorPillar(level, origin, rng);
            case "cryo_spire" -> buildCryoSpire(level, origin, rng);
            case "floating_island" -> buildFloatingIsland(level, origin, rng);
            case "registry_pylon" -> buildRegistryPylon(level, origin, rng);
            case "glitch_monolith" -> buildGlitchMonolith(level, origin, rng);
            default -> false;
        };
    }

    private boolean buildServerTower(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState basalt = SimpleAeDimension.SILICON_BASALT_BLOCK.get().defaultBlockState();
        BlockState server = SimpleAeDimension.SERVER_RACK_BLOCK.get().defaultBlockState();
        BlockState panel = SimpleAeDimension.CYBER_PANEL_BLOCK.get().defaultBlockState();
        BlockState proc = SimpleAeDimension.PROCESSOR_BLOCK.get().defaultBlockState();
        BlockState glass = SimpleAeDimension.CYBER_GLASS_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();

        int height = 10 + rng.nextInt(8);
        // Фундамент 5x5
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    level.setBlock(base.offset(dx, dy, dz), basalt, 2);
                }
            }
        }
        // Тело башни 3x3
        for (int y = 1; y <= height; y++) {
            boolean band = (y % 4 == 0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                    boolean center = dx == 0 && dz == 0;
                    BlockState st;
                    if (center) {
                        st = band ? glow : proc;
                    } else if (corner) {
                        st = band ? panel : basalt;
                    } else {
                        st = band ? glass : server;
                    }
                    level.setBlock(base.offset(dx, y, dz), st, 2);
                }
            }
        }
        // Шпиль и маяк на вершине
        level.setBlock(base.above(height + 1), proc, 2);
        level.setBlock(base.above(height + 2), glow, 2);
        level.setBlock(base.above(height + 3), glass, 2);
        return true;
    }

    private boolean buildCapacitorPillar(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState basalt = SimpleAeDimension.SILICON_BASALT_BLOCK.get().defaultBlockState();
        BlockState cond = SimpleAeDimension.CONDUCTIVE_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();
        BlockState panel = SimpleAeDimension.CYBER_PANEL_BLOCK.get().defaultBlockState();

        int h = 9 + rng.nextInt(7);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(base.offset(dx, -1, dz), basalt, 2);
                level.setBlock(base.offset(dx, 0, dz), (dx == 0 || dz == 0) ? cond : panel, 2);
            }
        }
        for (int y = 1; y <= h; y++) {
            level.setBlock(base.above(y), (y % 3 == 0) ? cond : basalt, 2);
            if (y == h - 2 || y == h - 5) {
                // Кольцо-резонатор
                level.setBlock(base.offset(1, y, 0), cond, 2);
                level.setBlock(base.offset(-1, y, 0), cond, 2);
                level.setBlock(base.offset(0, y, 1), cond, 2);
                level.setBlock(base.offset(0, y, -1), cond, 2);
            }
        }
        level.setBlock(base.above(h + 1), glow, 2);
        return true;
    }

    private boolean buildCryoSpire(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState cryo = SimpleAeDimension.CRYO_BASALT_BLOCK.get().defaultBlockState();
        BlockState glass = SimpleAeDimension.CYBER_GLASS_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();

        int h = 8 + rng.nextInt(8);
        for (int y = -1; y <= h; y++) {
            int rad = y < h / 3 ? 2 : (y < (h * 2) / 3 ? 1 : 0);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= rad + (rad == 2 ? 0 : 1)) {
                        BlockState st = (dx == 0 && dz == 0 && y % 2 == 0) ? glow : ((dx == 0 && dz == 0) ? glass : cryo);
                        level.setBlock(base.offset(dx, y, dz), st, 2);
                    }
                }
            }
        }
        level.setBlock(base.above(h + 1), glow, 2);
        return true;
    }

    private boolean buildFloatingIsland(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState plate = SimpleAeDimension.ARCHIVE_PLATE_BLOCK.get().defaultBlockState();
        BlockState panel = SimpleAeDimension.CYBER_PANEL_BLOCK.get().defaultBlockState();
        BlockState proc = SimpleAeDimension.PROCESSOR_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();
        BlockState coolant = SimpleAeDimension.COOLANT_BLOCK.get().defaultBlockState();

        BlockPos center = base.above(14 + rng.nextInt(8));
        int r = 3 + rng.nextInt(2);
        // Перевёрнутый конус острова
        for (int dy = -3; dy <= 0; dy++) {
            int curR = Math.max(0, r + dy);
            for (int dx = -curR; dx <= curR; dx++) {
                for (int dz = -curR; dz <= curR; dz++) {
                    if (dx * dx + dz * dz <= curR * curR + 1) {
                        BlockState st = (dy == 0) ? plate : (dy == -3 ? glow : panel);
                        level.setBlock(center.offset(dx, dy, dz), st, 2);
                    }
                }
            }
        }
        // Небольшой монолит и водопад хладагента с края острова
        level.setBlock(center.above(1), proc, 2);
        level.setBlock(center.above(2), glow, 2);
        level.setBlock(center.offset(r, 0, 0), coolant, 2);
        return true;
    }

    private boolean buildRegistryPylon(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState obs = SimpleAeDimension.REGISTRY_OBSIDIAN_BLOCK.get().defaultBlockState();
        BlockState cond = SimpleAeDimension.CONDUCTIVE_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();

        int h = 8 + rng.nextInt(5);
        // Арка-пилон с двумя колоннами и перемычкой
        for (int y = -1; y <= h; y++) {
            level.setBlock(base.offset(-2, y, 0), (y % 3 == 0) ? cond : obs, 2);
            level.setBlock(base.offset(2, y, 0), (y % 3 == 0) ? cond : obs, 2);
        }
        for (int dx = -2; dx <= 2; dx++) {
            level.setBlock(base.offset(dx, h, 0), obs, 2);
            level.setBlock(base.offset(dx, h + 1, 0), (dx == 0) ? glow : cond, 2);
        }
        level.setBlock(base.offset(0, h - 1, 0), glow, 2);
        return true;
    }

    private boolean buildGlitchMonolith(WorldGenLevel level, BlockPos base, RandomSource rng) {
        BlockState corr = SimpleAeDimension.CORRUPTED_STONE_BLOCK.get().defaultBlockState();
        BlockState leaves = SimpleAeDimension.VOID_LEAVES_BLOCK.get().defaultBlockState();
        BlockState glow = SimpleAeDimension.GLOW_CUBE_BLOCK.get().defaultBlockState();

        int h = 7 + rng.nextInt(6);
        BlockPos cur = base.above(2);
        for (int y = 0; y < h; y++) {
            int ox = rng.nextInt(3) - 1;
            int oz = rng.nextInt(3) - 1;
            level.setBlock(cur.offset(0, y, 0), (y % 2 == 0) ? corr : leaves, 2);
            level.setBlock(cur.offset(ox, y, oz), corr, 2);
        }
        level.setBlock(cur.above(h), glow, 2);
        return true;
    }
}
