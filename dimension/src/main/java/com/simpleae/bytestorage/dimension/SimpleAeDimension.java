package com.simpleae.bytestorage.dimension;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.dimension.block.CyberPlantBlock;
import com.simpleae.bytestorage.dimension.block.MagneticUpdraftBlock;
import com.simpleae.bytestorage.dimension.block.RiftGateCoreBlock;
import com.simpleae.bytestorage.dimension.block.RiftPortalBlock;
import com.simpleae.bytestorage.dimension.block.entity.RiftGateCoreBlockEntity;
import com.simpleae.bytestorage.dimension.client.SiliconPlaneSkyEffects;
import com.simpleae.bytestorage.dimension.client.renderer.ByteSentinelRenderer;
import com.simpleae.bytestorage.dimension.client.renderer.CryoHarvesterRenderer;
import com.simpleae.bytestorage.dimension.client.renderer.GlitchStalkerRenderer;
import com.simpleae.bytestorage.dimension.client.renderer.RegistryWardenRenderer;
import com.simpleae.bytestorage.dimension.entity.ByteSentinelEntity;
import com.simpleae.bytestorage.dimension.entity.CryoHarvesterEntity;
import com.simpleae.bytestorage.dimension.entity.GlitchStalkerEntity;
import com.simpleae.bytestorage.dimension.entity.RegistryWardenEntity;
import com.simpleae.bytestorage.dimension.fluid.CoolantFluidType;
import com.simpleae.bytestorage.dimension.item.PhaseStabilizerItem;
import com.simpleae.bytestorage.dimension.item.RiftCompassItem;
import com.simpleae.bytestorage.dimension.item.RiftResonatorItem;
import com.simpleae.bytestorage.dimension.world.SiliconEnvironmentHandler;
import com.simpleae.bytestorage.dimension.world.SiliconPlaneDimension;
import com.simpleae.bytestorage.dimension.world.feature.CyberStructureFeature;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import com.simpleae.bytestorage.item.ByteStorageCellItem;
import com.simpleae.bytestorage.item.EnergyCellItem;
import com.simpleae.bytestorage.item.FluidCellItem;
import com.simpleae.bytestorage.item.PortableByteCellItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Simple Byte Storage: Silicon Plane — необязательный аддон измерения.
 * Этапы 1–2: Врата Разлома (работают только в Silicon Rift + ачивка «Сигнал не ловит»),
 * полная карта из 8 уникальных биомов, 7 руд, Жидкий хладагент и эндгейм-ячейки (256k, 1M, 64k Портативная/Энерго, 256k Жидкостная).
 */
@Mod(SimpleAeDimension.MOD_ID)
public class SimpleAeDimension {

    public static final String MOD_ID = "simple_bytestorage_dimension";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Feature<CyberStructureFeature.Config>> CYBER_STRUCTURE_FEATURE =
            FEATURES.register("cyber_structure", CyberStructureFeature::new);

    public static final RegistryObject<EntityType<ByteSentinelEntity>> BYTE_SENTINEL =
            ENTITIES.register("byte_sentinel", () -> EntityType.Builder.of(ByteSentinelEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.3F).build("byte_sentinel"));

    public static final RegistryObject<EntityType<GlitchStalkerEntity>> GLITCH_STALKER =
            ENTITIES.register("glitch_stalker", () -> EntityType.Builder.of(GlitchStalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.65F, 2.2F).build("glitch_stalker"));

    public static final RegistryObject<EntityType<CryoHarvesterEntity>> CRYO_HARVESTER =
            ENTITIES.register("cryo_harvester", () -> EntityType.Builder.of(CryoHarvesterEntity::new, MobCategory.CREATURE)
                    .sized(1.15F, 2.1F).build("cryo_harvester"));

    public static final RegistryObject<EntityType<RegistryWardenEntity>> REGISTRY_WARDEN =
            ENTITIES.register("registry_warden", () -> EntityType.Builder.of(RegistryWardenEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 2.7F).fireImmune().build("registry_warden"));

    // --- Жидкий хладагент (Liquid Coolant) ---
    public static final RegistryObject<FluidType> COOLANT_FLUID_TYPE = FLUID_TYPES.register("liquid_coolant",
            () -> new CoolantFluidType(FluidType.Properties.create()
                    .lightLevel(8)
                    .density(1200)
                    .viscosity(1500)
                    .temperature(220)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final RegistryObject<FlowingFluid> COOLANT_SOURCE = FLUIDS.register("liquid_coolant",
            () -> new ForgeFlowingFluid.Source(coolantProperties()));

    public static final RegistryObject<FlowingFluid> COOLANT_FLOWING = FLUIDS.register("flowing_liquid_coolant",
            () -> new ForgeFlowingFluid.Flowing(coolantProperties()));

    public static final RegistryObject<LiquidBlock> COOLANT_BLOCK = BLOCKS.register("liquid_coolant",
            () -> new LiquidBlock(COOLANT_SOURCE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .lightLevel(s -> 8)
                    .sound(SoundType.EMPTY)));

    public static final RegistryObject<Item> COOLANT_BUCKET = ITEMS.register("coolant_bucket",
            () -> new BucketItem(COOLANT_SOURCE, new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));

    private static ForgeFlowingFluid.Properties coolantProperties() {
        return new ForgeFlowingFluid.Properties(COOLANT_FLUID_TYPE, COOLANT_SOURCE, COOLANT_FLOWING)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(1)
                .block(COOLANT_BLOCK)
                .bucket(COOLANT_BUCKET);
    }

    // --- Породы и строительные блоки 8 биомов ---
    public static final RegistryObject<Block> SILICON_BASALT_BLOCK = BLOCKS.register("silicon_basalt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(4.0F, 9.0F).sound(SoundType.BASALT)));
    public static final RegistryObject<Item> SILICON_BASALT_ITEM = blockItem("silicon_basalt", SILICON_BASALT_BLOCK);

    public static final RegistryObject<Block> SILICON_SLATE_BLOCK = BLOCKS.register("silicon_slate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops()
                    .strength(2.8F, 6.0F).sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Item> SILICON_SLATE_ITEM = blockItem("silicon_slate", SILICON_SLATE_BLOCK);

    public static final RegistryObject<Block> CRYO_BASALT_BLOCK = BLOCKS.register("cryo_basalt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE).requiresCorrectToolForDrops()
                    .strength(3.2F, 7.0F).friction(0.88F).lightLevel(s -> 3).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> CRYO_BASALT_ITEM = blockItem("cryo_basalt", CRYO_BASALT_BLOCK);

    public static final RegistryObject<Block> CORRUPTED_STONE_BLOCK = BLOCKS.register("corrupted_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(3.5F, 8.0F).lightLevel(s -> 4).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> CORRUPTED_STONE_ITEM = blockItem("corrupted_stone", CORRUPTED_STONE_BLOCK);

    public static final RegistryObject<Block> ARCHIVE_PLATE_BLOCK = BLOCKS.register("archive_plate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops()
                    .strength(4.0F, 10.0F).lightLevel(s -> 6).sound(SoundType.METAL)));
    public static final RegistryObject<Item> ARCHIVE_PLATE_ITEM = blockItem("archive_plate", ARCHIVE_PLATE_BLOCK);

    public static final RegistryObject<Block> REGISTRY_OBSIDIAN_BLOCK = BLOCKS.register("registry_obsidian",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(12.0F, 1200.0F).lightLevel(s -> 5).sound(SoundType.STONE)));
    public static final RegistryObject<Item> REGISTRY_OBSIDIAN_ITEM = blockItem("registry_obsidian", REGISTRY_OBSIDIAN_BLOCK);

    public static final RegistryObject<Block> OPTIC_LOG_BLOCK = BLOCKS.register("optic_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).strength(2.5F).lightLevel(s -> 4).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> OPTIC_LOG_ITEM = blockItem("optic_log", OPTIC_LOG_BLOCK);

    public static final RegistryObject<Block> OPTIC_LEAVES_BLOCK = BLOCKS.register("optic_leaves",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND).strength(0.6F).lightLevel(s -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> OPTIC_LEAVES_ITEM = blockItem("optic_leaves", OPTIC_LEAVES_BLOCK);

    // --- Кибер-деревья (пустотное фиолетовое и светящееся крио) ---
    public static final RegistryObject<Block> VOID_LOG_BLOCK = BLOCKS.register("void_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(2.6F).lightLevel(s -> 5).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> VOID_LOG_ITEM = blockItem("void_log", VOID_LOG_BLOCK);

    public static final RegistryObject<Block> VOID_LEAVES_BLOCK = BLOCKS.register("void_leaves",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_MAGENTA).strength(0.6F).lightLevel(s -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> VOID_LEAVES_ITEM = blockItem("void_leaves", VOID_LEAVES_BLOCK);

    public static final RegistryObject<Block> CRYO_LOG_BLOCK = BLOCKS.register("cryo_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE).strength(2.6F).lightLevel(s -> 6).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> CRYO_LOG_ITEM = blockItem("cryo_log", CRYO_LOG_BLOCK);

    public static final RegistryObject<Block> CRYO_LEAVES_BLOCK = BLOCKS.register("cryo_leaves",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW).strength(0.6F).lightLevel(s -> 12).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> CRYO_LEAVES_ITEM = blockItem("cryo_leaves", CRYO_LEAVES_BLOCK);

    // --- Архитектурные техно-блоки (для башен, руин, пилонов и строительства) ---
    public static final RegistryObject<Block> SERVER_RACK_BLOCK = BLOCKS.register("server_rack",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(3.5F, 8.0F).lightLevel(s -> 8).sound(SoundType.METAL)));
    public static final RegistryObject<Item> SERVER_RACK_ITEM = blockItem("server_rack", SERVER_RACK_BLOCK);

    public static final RegistryObject<Block> PROCESSOR_BLOCK = BLOCKS.register("processor_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(3.8F, 9.0F).lightLevel(s -> 10).sound(SoundType.METAL)));
    public static final RegistryObject<Item> PROCESSOR_BLOCK_ITEM = blockItem("processor_block", PROCESSOR_BLOCK);

    public static final RegistryObject<Block> CYBER_PANEL_BLOCK = BLOCKS.register("cyber_panel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(3.2F, 8.0F).lightLevel(s -> 5).sound(SoundType.METAL)));
    public static final RegistryObject<Item> CYBER_PANEL_ITEM = blockItem("cyber_panel", CYBER_PANEL_BLOCK);

    public static final RegistryObject<Block> GLOW_CUBE_BLOCK = BLOCKS.register("glow_cube",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .strength(1.8F, 6.0F).lightLevel(s -> 15).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> GLOW_CUBE_ITEM = blockItem("glow_cube", GLOW_CUBE_BLOCK);

    public static final RegistryObject<Block> CONDUCTIVE_BLOCK = BLOCKS.register("conductive_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops()
                    .strength(4.0F, 9.0F).lightLevel(s -> 10).sound(SoundType.METAL)));
    public static final RegistryObject<Item> CONDUCTIVE_BLOCK_ITEM = blockItem("conductive_block", CONDUCTIVE_BLOCK);

    public static final RegistryObject<Block> CYBER_GLASS_BLOCK = BLOCKS.register("cyber_glass",
            () -> new GlassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).noOcclusion()
                    .strength(1.2F).lightLevel(s -> 6).sound(SoundType.GLASS)
                    .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final RegistryObject<Item> CYBER_GLASS_ITEM = blockItem("cyber_glass", CYBER_GLASS_BLOCK);

    // --- Светящаяся цифровая растительность ---
    public static final RegistryObject<Block> GLOW_GRASS_BLOCK = BLOCKS.register("glow_grass",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).noCollission().instabreak()
                    .lightLevel(s -> 7).sound(SoundType.GRASS)));
    public static final RegistryObject<Item> GLOW_GRASS_ITEM = blockItem("glow_grass", GLOW_GRASS_BLOCK);

    public static final RegistryObject<Block> DIGITAL_FLOWER_BLOCK = BLOCKS.register("digital_flower",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak()
                    .lightLevel(s -> 9).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> DIGITAL_FLOWER_ITEM = blockItem("digital_flower", DIGITAL_FLOWER_BLOCK);

    public static final RegistryObject<Block> CRYSTAL_BUSH_BLOCK = BLOCKS.register("crystal_bush",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND).noCollission().instabreak()
                    .lightLevel(s -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> CRYSTAL_BUSH_ITEM = blockItem("crystal_bush", CRYSTAL_BUSH_BLOCK);

    public static final RegistryObject<Block> PLASMA_STALK_BLOCK = BLOCKS.register("plasma_stalk",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission().instabreak()
                    .lightLevel(s -> 11).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> PLASMA_STALK_ITEM = blockItem("plasma_stalk", PLASMA_STALK_BLOCK);

    public static final RegistryObject<Block> MAGNETIC_UPDRAFT_BLOCK = BLOCKS.register("magnetic_updraft",
            () -> new MagneticUpdraftBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).noCollission().replaceable()
                    .strength(-1.0F).lightLevel(s -> 11).sound(SoundType.GLASS).noLootTable()));
    public static final RegistryObject<Item> MAGNETIC_UPDRAFT_ITEM = blockItem("magnetic_updraft", MAGNETIC_UPDRAFT_BLOCK);

    // --- 7 уникальных руд измерения ---
    public static final RegistryObject<Block> BUSIUM_ORE_BLOCK = oreBlock("busium_ore", 4.0F, 5, 2, 5);
    public static final RegistryObject<Item> BUSIUM_ORE_ITEM = blockItem("busium_ore", BUSIUM_ORE_BLOCK);

    public static final RegistryObject<Block> OPTIC_QUARTZ_ORE_BLOCK = oreBlock("optic_quartz_ore", 3.5F, 7, 3, 6);
    public static final RegistryObject<Item> OPTIC_QUARTZ_ORE_ITEM = blockItem("optic_quartz_ore", OPTIC_QUARTZ_ORE_BLOCK);

    public static final RegistryObject<Block> CRYO_SILICATE_ORE_BLOCK = oreBlock("cryo_silicate_ore", 4.5F, 7, 3, 6);
    public static final RegistryObject<Item> CRYO_SILICATE_ORE_ITEM = blockItem("cryo_silicate_ore", CRYO_SILICATE_ORE_BLOCK);

    public static final RegistryObject<Block> FLUXITE_ORE_BLOCK = oreBlock("fluxite_ore", 4.5F, 9, 4, 7);
    public static final RegistryObject<Item> FLUXITE_ORE_ITEM = blockItem("fluxite_ore", FLUXITE_ORE_BLOCK);

    public static final RegistryObject<Block> ENTROPY_ORE_BLOCK = oreBlock("entropy_ore", 5.0F, 8, 4, 8);
    public static final RegistryObject<Item> ENTROPY_ORE_ITEM = blockItem("entropy_ore", ENTROPY_ORE_BLOCK);

    public static final RegistryObject<Block> QUANTUMITE_ORE_BLOCK = oreBlock("quantumite_ore", 6.0F, 10, 5, 9);
    public static final RegistryObject<Item> QUANTUMITE_ORE_ITEM = blockItem("quantumite_ore", QUANTUMITE_ORE_BLOCK);

    public static final RegistryObject<Block> MATRIX_ORE_BLOCK = oreBlock("matrix_ore", 8.0F, 12, 6, 12);
    public static final RegistryObject<Item> MATRIX_ORE_ITEM = blockItem("matrix_ore", MATRIX_ORE_BLOCK);

    // --- Врата Разлома ---
    public static final RegistryObject<Block> RIFT_GATE_CORE_BLOCK = BLOCKS.register("rift_gate_core",
            () -> new RiftGateCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(5.0F, 12.0F).lightLevel(s -> 9).sound(SoundType.METAL)));
    public static final RegistryObject<Item> RIFT_GATE_CORE_ITEM = blockItem("rift_gate_core", RIFT_GATE_CORE_BLOCK);
    public static final RegistryObject<BlockEntityType<RiftGateCoreBlockEntity>> RIFT_GATE_CORE_ENTITY =
            BLOCK_ENTITIES.register("rift_gate_core",
                    () -> BlockEntityType.Builder.of(RiftGateCoreBlockEntity::new, RIFT_GATE_CORE_BLOCK.get()).build(null));

    public static final RegistryObject<Block> RIFT_PORTAL_BLOCK = BLOCKS.register("rift_portal",
            () -> new RiftPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).noCollission().strength(-1.0F)
                    .sound(SoundType.GLASS).lightLevel(s -> 12).noLootTable()));

    // --- Материалы, слитки, кристаллы ---
    public static final RegistryObject<Item> RAW_BUSIUM = simpleItem("raw_busium");
    public static final RegistryObject<Item> BUSIUM_INGOT = simpleItem("busium_ingot");
    public static final RegistryObject<Item> OPTIC_QUARTZ = simpleItem("optic_quartz");
    public static final RegistryObject<Item> BUSIUM_PROCESSOR = simpleItem("busium_processor");

    public static final RegistryObject<Item> CRYO_SILICATE = simpleItem("cryo_silicate");
    public static final RegistryObject<Item> FLUXITE_DUST = simpleItem("fluxite_dust");
    public static final RegistryObject<Item> ENTROPY_SHARD = simpleItem("entropy_shard");
    public static final RegistryObject<Item> RAW_QUANTUMITE = simpleItem("raw_quantumite");
    public static final RegistryObject<Item> QUANTUMITE_INGOT = simpleItem("quantumite_ingot");
    public static final RegistryObject<Item> MATRIX_CRYSTAL = simpleItem("matrix_crystal");
    public static final RegistryObject<Item> QUANTUM_CORE = simpleItem("quantum_core");

    public static final RegistryObject<Item> RIFT_RESONATOR = ITEMS.register("rift_resonator",
            () -> new RiftResonatorItem(new Item.Properties()));
    public static final RegistryObject<Item> RIFT_COMPASS = ITEMS.register("rift_compass",
            () -> new RiftCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> PHASE_STABILIZER = ITEMS.register("phase_stabilizer",
            () -> new PhaseStabilizerItem(new Item.Properties()));
    public static final RegistryObject<Item> BYTE_SENTINEL_SPAWN_EGG = ITEMS.register("byte_sentinel_spawn_egg",
            () -> new ForgeSpawnEggItem(BYTE_SENTINEL, 0x1A2638, 0x2AE6F8, new Item.Properties()));
    public static final RegistryObject<Item> GLITCH_STALKER_SPAWN_EGG = ITEMS.register("glitch_stalker_spawn_egg",
            () -> new ForgeSpawnEggItem(GLITCH_STALKER, 0x241038, 0xC844FF, new Item.Properties()));
    public static final RegistryObject<Item> CRYO_HARVESTER_SPAWN_EGG = ITEMS.register("cryo_harvester_spawn_egg",
            () -> new ForgeSpawnEggItem(CRYO_HARVESTER, 0x2A4E6E, 0x8CEEFF, new Item.Properties()));
    public static final RegistryObject<Item> REGISTRY_WARDEN_SPAWN_EGG = ITEMS.register("registry_warden_spawn_egg",
            () -> new ForgeSpawnEggItem(REGISTRY_WARDEN, 0x1E1418, 0xFFD048, new Item.Properties()));

    // --- Высокоуровневые ячейки хранения (Тиры 2–4) ---
    public static final RegistryObject<Item> CELL_256K = ITEMS.register("cell_256k",
            () -> new ByteStorageCellItem(CellTier.TIER_256K, new Item.Properties()));
    public static final RegistryObject<Item> CELL_1M = ITEMS.register("cell_1m",
            () -> new ByteStorageCellItem(CellTier.TIER_1M, new Item.Properties()));
    public static final RegistryObject<Item> PORTABLE_CELL_64K = ITEMS.register("portable_cell_64k",
            () -> new PortableByteCellItem(CellTier.TIER_64K, new Item.Properties()));
    public static final RegistryObject<Item> ENERGY_CELL_64K = ITEMS.register("energy_cell_64k",
            () -> new EnergyCellItem(CellTier.TIER_64K, new Item.Properties()));
    public static final RegistryObject<Item> FLUID_CELL_256K = ITEMS.register("fluid_cell_256k",
            () -> new FluidCellItem(CellTier.TIER_256K, new Item.Properties()));

    // --- Вкладка креатива ---
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("simple_bytestorage_dimension_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_bytestorage_dimension"))
                    .icon(() -> new ItemStack(RIFT_GATE_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RIFT_GATE_CORE_ITEM.get());
                        output.accept(SILICON_BASALT_ITEM.get());
                        output.accept(RIFT_RESONATOR.get());
                        output.accept(RIFT_COMPASS.get());
                        output.accept(PHASE_STABILIZER.get());
                        output.accept(BYTE_SENTINEL_SPAWN_EGG.get());
                        output.accept(GLITCH_STALKER_SPAWN_EGG.get());
                        output.accept(CRYO_HARVESTER_SPAWN_EGG.get());
                        output.accept(REGISTRY_WARDEN_SPAWN_EGG.get());
                        output.accept(SILICON_SLATE_ITEM.get());
                        output.accept(CRYO_BASALT_ITEM.get());
                        output.accept(CORRUPTED_STONE_ITEM.get());
                        output.accept(ARCHIVE_PLATE_ITEM.get());
                        output.accept(REGISTRY_OBSIDIAN_ITEM.get());
                        output.accept(OPTIC_LOG_ITEM.get());
                        output.accept(OPTIC_LEAVES_ITEM.get());
                        output.accept(VOID_LOG_ITEM.get());
                        output.accept(VOID_LEAVES_ITEM.get());
                        output.accept(CRYO_LOG_ITEM.get());
                        output.accept(CRYO_LEAVES_ITEM.get());
                        output.accept(SERVER_RACK_ITEM.get());
                        output.accept(PROCESSOR_BLOCK_ITEM.get());
                        output.accept(CYBER_PANEL_ITEM.get());
                        output.accept(GLOW_CUBE_ITEM.get());
                        output.accept(CONDUCTIVE_BLOCK_ITEM.get());
                        output.accept(CYBER_GLASS_ITEM.get());
                        output.accept(GLOW_GRASS_ITEM.get());
                        output.accept(DIGITAL_FLOWER_ITEM.get());
                        output.accept(CRYSTAL_BUSH_ITEM.get());
                        output.accept(PLASMA_STALK_ITEM.get());
                        output.accept(BUSIUM_ORE_ITEM.get());
                        output.accept(OPTIC_QUARTZ_ORE_ITEM.get());
                        output.accept(CRYO_SILICATE_ORE_ITEM.get());
                        output.accept(FLUXITE_ORE_ITEM.get());
                        output.accept(ENTROPY_ORE_ITEM.get());
                        output.accept(QUANTUMITE_ORE_ITEM.get());
                        output.accept(MATRIX_ORE_ITEM.get());
                        output.accept(RAW_BUSIUM.get());
                        output.accept(BUSIUM_INGOT.get());
                        output.accept(OPTIC_QUARTZ.get());
                        output.accept(BUSIUM_PROCESSOR.get());
                        output.accept(CRYO_SILICATE.get());
                        output.accept(FLUXITE_DUST.get());
                        output.accept(ENTROPY_SHARD.get());
                        output.accept(RAW_QUANTUMITE.get());
                        output.accept(QUANTUMITE_INGOT.get());
                        output.accept(MATRIX_CRYSTAL.get());
                        output.accept(QUANTUM_CORE.get());
                        output.accept(COOLANT_BUCKET.get());
                        output.accept(CELL_256K.get());
                        output.accept(CELL_1M.get());
                        output.accept(PORTABLE_CELL_64K.get());
                        output.accept(ENERGY_CELL_64K.get());
                        output.accept(FLUID_CELL_256K.get());
                    })
                    .build());

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Block> oreBlock(String name, float strength, int light, int minXp, int maxXp) {
        return BLOCKS.register(name, () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops()
                .strength(strength, 8.0F).lightLevel(s -> light).sound(SoundType.DEEPSLATE),
                UniformInt.of(minXp, maxXp)));
    }

    public SimpleAeDimension() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
        FEATURES.register(modEventBus);
        ENTITIES.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DimensionConfig.SPEC);
        modEventBus.addListener(this::registerEntityAttributes);
        MinecraftForge.EVENT_BUS.register(new SiliconEnvironmentHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerDimensionEffects);
            modEventBus.addListener(this::registerLayerDefinitions);
            modEventBus.addListener(this::registerEntityRenderers);
        }
    }

    private void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(BYTE_SENTINEL.get(), ByteSentinelEntity.createAttributes().build());
        event.put(GLITCH_STALKER.get(), GlitchStalkerEntity.createAttributes().build());
        event.put(CRYO_HARVESTER.get(), CryoHarvesterEntity.createAttributes().build());
        event.put(REGISTRY_WARDEN.get(), RegistryWardenEntity.createAttributes().build());
    }

    private void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ByteSentinelRenderer.LAYER, ByteSentinelRenderer.SentinelModel::createBodyLayer);
        event.registerLayerDefinition(GlitchStalkerRenderer.LAYER, GlitchStalkerRenderer.StalkerModel::createBodyLayer);
        event.registerLayerDefinition(CryoHarvesterRenderer.LAYER, CryoHarvesterRenderer.HarvesterModel::createBodyLayer);
        event.registerLayerDefinition(RegistryWardenRenderer.LAYER, RegistryWardenRenderer.WardenModel::createBodyLayer);
    }

    private void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BYTE_SENTINEL.get(), ByteSentinelRenderer::new);
        event.registerEntityRenderer(GLITCH_STALKER.get(), GlitchStalkerRenderer::new);
        event.registerEntityRenderer(CRYO_HARVESTER.get(), CryoHarvesterRenderer::new);
        event.registerEntityRenderer(REGISTRY_WARDEN.get(), RegistryWardenRenderer::new);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.simpleae.bytestorage.client.guide.GuideRegistry.register(
                    new com.simpleae.bytestorage.client.guide.GuideRegistry.Chapter(
                            "dimension_portal", 200,
                            "guide.simple_bytestorage_dimension.portal.title",
                            "guide.simple_bytestorage_dimension.portal.body",
                            "simple_bytestorage_dimension:rift_gate_core",
                            List.of(
                                    "simple_bytestorage_dimension:rift_gate_core",
                                    "simple_bytestorage_dimension:silicon_basalt",
                                    "simple_bytestorage_dimension:rift_resonator",
                                    "simple_bytestorage_dimension:rift_compass"
                            )
                    ));
            com.simpleae.bytestorage.client.guide.GuideRegistry.register(
                    new com.simpleae.bytestorage.client.guide.GuideRegistry.Chapter(
                            "dimension_resources", 210,
                            "guide.simple_bytestorage_dimension.resources.title",
                            "guide.simple_bytestorage_dimension.resources.body",
                            "simple_bytestorage_dimension:cell_256k",
                            List.of(
                                    "simple_bytestorage_dimension:busium_ingot",
                                    "simple_bytestorage_dimension:busium_processor",
                                    "simple_bytestorage_dimension:cell_256k",
                                    "simple_bytestorage_dimension:portable_cell_64k"
                            )
                    ));
            com.simpleae.bytestorage.client.guide.GuideRegistry.register(
                    new com.simpleae.bytestorage.client.guide.GuideRegistry.Chapter(
                            "dimension_endgame", 220,
                            "guide.simple_bytestorage_dimension.endgame.title",
                            "guide.simple_bytestorage_dimension.endgame.body",
                            "simple_bytestorage_dimension:cell_1m",
                            List.of(
                                    "simple_bytestorage_dimension:quantumite_ingot",
                                    "simple_bytestorage_dimension:quantum_core",
                                    "simple_bytestorage_dimension:cell_1m",
                                    "simple_bytestorage_dimension:energy_cell_64k",
                                    "simple_bytestorage_dimension:fluid_cell_256k"
                            )
                    ));
        });
    }

    private void registerDimensionEffects(final RegisterDimensionSpecialEffectsEvent event) {
        event.register(SiliconPlaneDimension.SILICON_PLANE_ID, new SiliconPlaneSkyEffects());
    }
}
