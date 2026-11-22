package com.simpleae.bytestorage.dimension;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.dimension.block.CyberPlantBlock;
import com.simpleae.bytestorage.dimension.block.InterdimensionalEnergyTransmitterBlock;
import com.simpleae.bytestorage.dimension.block.MagneticUpdraftBlock;
import com.simpleae.bytestorage.dimension.block.OpticSaplingBlock;
import com.simpleae.bytestorage.dimension.block.PatternBusMk2Block;
import com.simpleae.bytestorage.dimension.block.QuantumRelayBlock;
import com.simpleae.bytestorage.dimension.block.ReturnBeaconBlock;
import com.simpleae.bytestorage.dimension.block.RiftGateCoreBlock;
import com.simpleae.bytestorage.dimension.block.RiftPortalBlock;
import com.simpleae.bytestorage.dimension.block.SingularControllerBlock;
import com.simpleae.bytestorage.dimension.block.TieredAssemblerBlock;
import com.simpleae.bytestorage.dimension.block.entity.InterdimensionalEnergyTransmitterBlockEntity;
import com.simpleae.bytestorage.dimension.block.entity.PatternBusMk2BlockEntity;
import com.simpleae.bytestorage.dimension.block.entity.QuantumRelayBlockEntity;
import com.simpleae.bytestorage.dimension.block.entity.RiftGateCoreBlockEntity;
import com.simpleae.bytestorage.dimension.block.entity.SingularControllerBlockEntity;
import com.simpleae.bytestorage.dimension.block.entity.TieredAssemblerBlockEntity;
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
import com.simpleae.bytestorage.dimension.item.ArchiveLoreItem;
import com.simpleae.bytestorage.dimension.item.CapacitorAccumulatorItem;
import com.simpleae.bytestorage.dimension.item.FacadeScannerItem;
import com.simpleae.bytestorage.dimension.item.PhaseStabilizerItem;
import com.simpleae.bytestorage.dimension.item.RiftCompassItem;
import com.simpleae.bytestorage.dimension.item.RiftResonatorItem;
import com.simpleae.bytestorage.dimension.item.SpeedCardMk2Item;
import com.simpleae.bytestorage.item.WirelessUpgradeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
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

    public static final RegistryObject<Block> RETURN_BEACON_BLOCK = BLOCKS.register("return_beacon",
            () -> new ReturnBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(4.5F, 12.0F).lightLevel(s -> 14).sound(SoundType.METAL)));
    public static final RegistryObject<Item> RETURN_BEACON_ITEM = blockItem("return_beacon", RETURN_BEACON_BLOCK);

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
    public static final RegistryObject<Item> CAPACITOR_ACCUMULATOR = ITEMS.register("capacitor_accumulator",
            () -> new CapacitorAccumulatorItem(new Item.Properties()));
    public static final RegistryObject<Item> ARCHIVE_LORE_FRAGMENT = ITEMS.register("archive_lore_fragment",
            () -> new ArchiveLoreItem(new Item.Properties()));
    public static final RegistryObject<Item> BUSIUM_PICKAXE = ITEMS.register("busium_pickaxe",
            () -> new PickaxeItem(Tiers.DIAMOND, 2, -2.6F, new Item.Properties().durability(1800)));
    public static final RegistryObject<Item> CRYO_PICKAXE = ITEMS.register("cryo_pickaxe",
            () -> new PickaxeItem(Tiers.NETHERITE, 3, -2.4F, new Item.Properties().durability(2600).fireResistant()));
    public static final RegistryObject<Item> ENERGY_CELL_256K = ITEMS.register("energy_cell_256k",
            () -> new EnergyCellItem(CellTier.TIER_256K, new Item.Properties()));
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

    // --- Эндгейм сетевые машины и апгрейды ---
    public static final RegistryObject<Block> ADVANCED_ASSEMBLER_BLOCK = BLOCKS.register("advanced_assembler",
            () -> new TieredAssemblerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(4.5F, 12.0F).lightLevel(s -> 10).sound(SoundType.METAL), false));
    public static final RegistryObject<Item> ADVANCED_ASSEMBLER_ITEM = blockItem("advanced_assembler", ADVANCED_ASSEMBLER_BLOCK);
    public static final RegistryObject<BlockEntityType<TieredAssemblerBlockEntity>> ADVANCED_ASSEMBLER_ENTITY =
            BLOCK_ENTITIES.register("advanced_assembler",
                    () -> BlockEntityType.Builder.of(TieredAssemblerBlockEntity::createAdvanced, ADVANCED_ASSEMBLER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> SINGULAR_ASSEMBLER_BLOCK = BLOCKS.register("singular_assembler",
            () -> new TieredAssemblerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(6.0F, 24.0F).lightLevel(s -> 14).sound(SoundType.METAL), true));
    public static final RegistryObject<Item> SINGULAR_ASSEMBLER_ITEM = blockItem("singular_assembler", SINGULAR_ASSEMBLER_BLOCK);
    public static final RegistryObject<BlockEntityType<TieredAssemblerBlockEntity>> SINGULAR_ASSEMBLER_ENTITY =
            BLOCK_ENTITIES.register("singular_assembler",
                    () -> BlockEntityType.Builder.of(TieredAssemblerBlockEntity::createSingular, SINGULAR_ASSEMBLER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> PATTERN_BUS_MK2_BLOCK = BLOCKS.register("pattern_bus_mk2",
            () -> new PatternBusMk2Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(4.5F, 12.0F).lightLevel(s -> 9).sound(SoundType.METAL)));
    public static final RegistryObject<Item> PATTERN_BUS_MK2_ITEM = blockItem("pattern_bus_mk2", PATTERN_BUS_MK2_BLOCK);
    public static final RegistryObject<BlockEntityType<PatternBusMk2BlockEntity>> PATTERN_BUS_MK2_ENTITY =
            BLOCK_ENTITIES.register("pattern_bus_mk2",
                    () -> BlockEntityType.Builder.of(PatternBusMk2BlockEntity::new, PATTERN_BUS_MK2_BLOCK.get()).build(null));

    public static final RegistryObject<Block> SINGULAR_CONTROLLER_BLOCK = BLOCKS.register("singular_controller",
            () -> new SingularControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(8.0F, 50.0F).lightLevel(s -> 15).sound(SoundType.METAL)));
    public static final RegistryObject<Item> SINGULAR_CONTROLLER_ITEM = blockItem("singular_controller", SINGULAR_CONTROLLER_BLOCK);
    public static final RegistryObject<BlockEntityType<SingularControllerBlockEntity>> SINGULAR_CONTROLLER_ENTITY =
            BLOCK_ENTITIES.register("singular_controller",
                    () -> BlockEntityType.Builder.of(SingularControllerBlockEntity::new, SINGULAR_CONTROLLER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> ENERGY_TRANSMITTER_BLOCK = BLOCKS.register("interdimensional_energy_transmitter",
            () -> new InterdimensionalEnergyTransmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops()
                    .strength(5.0F, 18.0F).lightLevel(s -> 12).sound(SoundType.METAL)));
    public static final RegistryObject<Item> ENERGY_TRANSMITTER_ITEM = blockItem("interdimensional_energy_transmitter", ENERGY_TRANSMITTER_BLOCK);
    public static final RegistryObject<BlockEntityType<InterdimensionalEnergyTransmitterBlockEntity>> ENERGY_TRANSMITTER_ENTITY =
            BLOCK_ENTITIES.register("interdimensional_energy_transmitter",
                    () -> BlockEntityType.Builder.of(InterdimensionalEnergyTransmitterBlockEntity::new, ENERGY_TRANSMITTER_BLOCK.get()).build(null));

    public static final RegistryObject<Block> QUANTUM_RELAY_BLOCK = BLOCKS.register("quantum_relay",
            () -> new QuantumRelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(4.5F, 12.0F).lightLevel(s -> 11).sound(SoundType.METAL)));
    public static final RegistryObject<Item> QUANTUM_RELAY_ITEM = blockItem("quantum_relay", QUANTUM_RELAY_BLOCK);
    public static final RegistryObject<BlockEntityType<QuantumRelayBlockEntity>> QUANTUM_RELAY_ENTITY =
            BLOCK_ENTITIES.register("quantum_relay",
                    () -> BlockEntityType.Builder.of(QuantumRelayBlockEntity::new, QUANTUM_RELAY_BLOCK.get()).build(null));

    public static final RegistryObject<Item> SPEED_CARD_MK2 = ITEMS.register("speed_card_mk2",
            () -> new SpeedCardMk2Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> INTERDIMENSIONAL_WIRELESS_CARD = ITEMS.register("interdimensional_wireless_card",
            () -> new WirelessUpgradeItem(WirelessUpgradeItem.Type.INTERDIMENSIONAL, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FACADE_SCANNER = ITEMS.register("facade_scanner",
            () -> new FacadeScannerItem(new Item.Properties()));

    // --- Компактные блоки ресурсов ---
    public static final RegistryObject<Block> BUSIUM_BLOCK = BLOCKS.register("busium_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 10.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Item> BUSIUM_BLOCK_ITEM = blockItem("busium_block", BUSIUM_BLOCK);

    public static final RegistryObject<Block> OPTIC_QUARTZ_BLOCK = BLOCKS.register("optic_quartz_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(4.0F, 8.0F).lightLevel(s -> 7).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> OPTIC_QUARTZ_BLOCK_ITEM = blockItem("optic_quartz_block", OPTIC_QUARTZ_BLOCK);

    public static final RegistryObject<Block> CRYO_SILICATE_BLOCK = BLOCKS.register("cryo_silicate_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).requiresCorrectToolForDrops().strength(4.5F, 8.0F).friction(0.9F).lightLevel(s -> 6).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> CRYO_SILICATE_BLOCK_ITEM = blockItem("cryo_silicate_block", CRYO_SILICATE_BLOCK);

    public static final RegistryObject<Block> FLUXITE_BLOCK = BLOCKS.register("fluxite_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(4.5F, 9.0F).lightLevel(s -> 11).sound(SoundType.METAL)));
    public static final RegistryObject<Item> FLUXITE_BLOCK_ITEM = blockItem("fluxite_block", FLUXITE_BLOCK);

    public static final RegistryObject<Block> QUANTUMITE_BLOCK = BLOCKS.register("quantumite_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(7.0F, 24.0F).lightLevel(s -> 12).sound(SoundType.METAL)));
    public static final RegistryObject<Item> QUANTUMITE_BLOCK_ITEM = blockItem("quantumite_block", QUANTUMITE_BLOCK);

    public static final RegistryObject<Block> MATRIX_BLOCK = BLOCKS.register("matrix_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(8.0F, 40.0F).lightLevel(s -> 14).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> MATRIX_BLOCK_ITEM = blockItem("matrix_block", MATRIX_BLOCK);

    // --- Декоративные, природные и архитектурные блоки измерения ---
    public static final RegistryObject<Block> OPTIC_PLANKS_BLOCK = BLOCKS.register("optic_planks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.2F, 4.0F).lightLevel(s -> 4).sound(SoundType.WOOD)));
    public static final RegistryObject<Item> OPTIC_PLANKS_ITEM = blockItem("optic_planks", OPTIC_PLANKS_BLOCK);

    public static final RegistryObject<Block> OPTIC_SAPLING_BLOCK = BLOCKS.register("optic_sapling",
            () -> new OpticSaplingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission().instabreak().lightLevel(s -> 8).sound(SoundType.GRASS)));
    public static final RegistryObject<Item> OPTIC_SAPLING_ITEM = blockItem("optic_sapling", OPTIC_SAPLING_BLOCK);

    public static final RegistryObject<Block> DENSE_CRYO_ICE_BLOCK = BLOCKS.register("dense_cryo_ice",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).friction(0.98F).lightLevel(s -> 5).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> DENSE_CRYO_ICE_ITEM = blockItem("dense_cryo_ice", DENSE_CRYO_ICE_BLOCK);

    public static final RegistryObject<Block> CORRUPTED_VAULT_CASING_BLOCK = BLOCKS.register("corrupted_vault_casing",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(6.0F, 40.0F).lightLevel(s -> 6).sound(SoundType.METAL)));
    public static final RegistryObject<Item> CORRUPTED_VAULT_CASING_ITEM = blockItem("corrupted_vault_casing", CORRUPTED_VAULT_CASING_BLOCK);

    public static final RegistryObject<Block> COOLANT_TANK_NODE_BLOCK = BLOCKS.register("coolant_tank_node",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(3.5F, 8.0F).lightLevel(s -> 9).sound(SoundType.GLASS)));
    public static final RegistryObject<Item> COOLANT_TANK_NODE_ITEM = blockItem("coolant_tank_node", COOLANT_TANK_NODE_BLOCK);

    public static final RegistryObject<Block> ARCHIVE_PILLAR_BLOCK = BLOCKS.register("archive_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(4.0F, 10.0F).lightLevel(s -> 7).sound(SoundType.STONE)));
    public static final RegistryObject<Item> ARCHIVE_PILLAR_ITEM = blockItem("archive_pillar", ARCHIVE_PILLAR_BLOCK);

    public static final RegistryObject<Block> ARCHIVE_MONOLITH_BLOCK = BLOCKS.register("archive_monolith",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(4.5F, 12.0F).lightLevel(s -> 8).sound(SoundType.STONE)));
    public static final RegistryObject<Item> ARCHIVE_MONOLITH_ITEM = blockItem("archive_monolith", ARCHIVE_MONOLITH_BLOCK);

    public static final RegistryObject<Block> ARCHIVE_STONE_BLOCK = BLOCKS.register("archive_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(3.5F, 9.0F).lightLevel(s -> 4).sound(SoundType.STONE)));
    public static final RegistryObject<Item> ARCHIVE_STONE_ITEM = blockItem("archive_stone", ARCHIVE_STONE_BLOCK);

    public static final RegistryObject<Block> CAPACITOR_PILLAR_BLOCK = BLOCKS.register("capacitor_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(4.0F, 10.0F).lightLevel(s -> 11).sound(SoundType.METAL)));
    public static final RegistryObject<Item> CAPACITOR_PILLAR_ITEM = blockItem("capacitor_pillar", CAPACITOR_PILLAR_BLOCK);

    public static final RegistryObject<Block> CAPACITOR_PLATE_BLOCK = BLOCKS.register("capacitor_plate",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.5F, 9.0F).lightLevel(s -> 8).sound(SoundType.METAL)));
    public static final RegistryObject<Item> CAPACITOR_PLATE_ITEM = blockItem("capacitor_plate", CAPACITOR_PLATE_BLOCK);

    public static final RegistryObject<Block> CANYON_SLATE_BLOCK = BLOCKS.register("canyon_slate",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Item> CANYON_SLATE_ITEM = blockItem("canyon_slate", CANYON_SLATE_BLOCK);

    public static final RegistryObject<Block> SILICON_SLATE_BRICKS_BLOCK = BLOCKS.register("silicon_slate_bricks",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(3.2F, 7.0F).sound(SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Item> SILICON_SLATE_BRICKS_ITEM = blockItem("silicon_slate_bricks", SILICON_SLATE_BRICKS_BLOCK);

    public static final RegistryObject<Block> NULL_OBSIDIAN_BLOCK = BLOCKS.register("null_obsidian",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(15.0F, 1200.0F).lightLevel(s -> 4).sound(SoundType.STONE)));
    public static final RegistryObject<Item> NULL_OBSIDIAN_ITEM = blockItem("null_obsidian", NULL_OBSIDIAN_BLOCK);

    public static final RegistryObject<Block> NULL_PYLON_BLOCK = BLOCKS.register("null_pylon",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(8.0F, 600.0F).lightLevel(s -> 10).sound(SoundType.STONE)));
    public static final RegistryObject<Item> NULL_PYLON_ITEM = blockItem("null_pylon", NULL_PYLON_BLOCK);

    public static final RegistryObject<Block> ENTROPY_CLUSTER_BLOCK = BLOCKS.register("entropy_cluster",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().strength(1.0F).lightLevel(s -> 11).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> ENTROPY_CLUSTER_ITEM = blockItem("entropy_cluster", ENTROPY_CLUSTER_BLOCK);

    public static final RegistryObject<Block> QUANTUM_CLUSTER_BLOCK = BLOCKS.register("quantum_cluster",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission().strength(1.2F).lightLevel(s -> 13).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> QUANTUM_CLUSTER_ITEM = blockItem("quantum_cluster", QUANTUM_CLUSTER_BLOCK);

    public static final RegistryObject<Block> SILICON_CRYSTAL_CLUSTER_BLOCK = BLOCKS.register("silicon_crystal_cluster",
            () -> new CyberPlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).noCollission().strength(0.8F).lightLevel(s -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> SILICON_CRYSTAL_CLUSTER_ITEM = blockItem("silicon_crystal_cluster", SILICON_CRYSTAL_CLUSTER_BLOCK);

    // --- Дополнительные компоненты и материалы ---
    public static final RegistryObject<Item> OPTIC_PROCESSOR = simpleItem("optic_processor");
    public static final RegistryObject<Item> SINGULARITY_CORE = simpleItem("singularity_core");
    public static final RegistryObject<Item> CRYO_CORE = simpleItem("cryo_core");
    public static final RegistryObject<Item> SENTINEL_BATTERY = simpleItem("sentinel_battery");
    public static final RegistryObject<Item> CHARGED_DUST = simpleItem("charged_dust");
    public static final RegistryObject<Item> CORRUPTED_CELL = simpleItem("corrupted_cell");
    public static final RegistryObject<Item> FLUXITE_CRYSTAL = simpleItem("fluxite_crystal");
    public static final RegistryObject<Item> QUANTUMITE_SHARD = simpleItem("quantumite_shard");

    // --- Вкладка креатива ---
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("simple_bytestorage_dimension_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_bytestorage_dimension"))
                    .icon(() -> new ItemStack(RIFT_GATE_CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RIFT_GATE_CORE_ITEM.get());
                        output.accept(RETURN_BEACON_ITEM.get());
                        output.accept(SILICON_BASALT_ITEM.get());
                        output.accept(RIFT_RESONATOR.get());
                        output.accept(RIFT_COMPASS.get());
                        output.accept(PHASE_STABILIZER.get());
                        output.accept(CAPACITOR_ACCUMULATOR.get());
                        output.accept(BUSIUM_PICKAXE.get());
                        output.accept(CRYO_PICKAXE.get());
                        output.accept(ARCHIVE_LORE_FRAGMENT.get());
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
                        output.accept(ENERGY_CELL_256K.get());
                        output.accept(FLUID_CELL_256K.get());
                        output.accept(ADVANCED_ASSEMBLER_ITEM.get());
                        output.accept(SINGULAR_ASSEMBLER_ITEM.get());
                        output.accept(PATTERN_BUS_MK2_ITEM.get());
                        output.accept(SINGULAR_CONTROLLER_ITEM.get());
                        output.accept(ENERGY_TRANSMITTER_ITEM.get());
                        output.accept(QUANTUM_RELAY_ITEM.get());
                        output.accept(SPEED_CARD_MK2.get());
                        output.accept(INTERDIMENSIONAL_WIRELESS_CARD.get());
                        output.accept(FACADE_SCANNER.get());
                        output.accept(OPTIC_PROCESSOR.get());
                        output.accept(SINGULARITY_CORE.get());
                        output.accept(CRYO_CORE.get());
                        output.accept(SENTINEL_BATTERY.get());
                        output.accept(CHARGED_DUST.get());
                        output.accept(CORRUPTED_CELL.get());
                        output.accept(FLUXITE_CRYSTAL.get());
                        output.accept(QUANTUMITE_SHARD.get());
                        output.accept(BUSIUM_BLOCK_ITEM.get());
                        output.accept(OPTIC_QUARTZ_BLOCK_ITEM.get());
                        output.accept(CRYO_SILICATE_BLOCK_ITEM.get());
                        output.accept(FLUXITE_BLOCK_ITEM.get());
                        output.accept(QUANTUMITE_BLOCK_ITEM.get());
                        output.accept(MATRIX_BLOCK_ITEM.get());
                        output.accept(OPTIC_PLANKS_ITEM.get());
                        output.accept(OPTIC_SAPLING_ITEM.get());
                        output.accept(DENSE_CRYO_ICE_ITEM.get());
                        output.accept(CORRUPTED_VAULT_CASING_ITEM.get());
                        output.accept(COOLANT_TANK_NODE_ITEM.get());
                        output.accept(ARCHIVE_PILLAR_ITEM.get());
                        output.accept(ARCHIVE_MONOLITH_ITEM.get());
                        output.accept(ARCHIVE_STONE_ITEM.get());
                        output.accept(CAPACITOR_PILLAR_ITEM.get());
                        output.accept(CAPACITOR_PLATE_ITEM.get());
                        output.accept(CANYON_SLATE_ITEM.get());
                        output.accept(SILICON_SLATE_BRICKS_ITEM.get());
                        output.accept(NULL_OBSIDIAN_ITEM.get());
                        output.accept(NULL_PYLON_ITEM.get());
                        output.accept(ENTROPY_CLUSTER_ITEM.get());
                        output.accept(QUANTUM_CLUSTER_ITEM.get());
                        output.accept(SILICON_CRYSTAL_CLUSTER_ITEM.get());
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
                                    "simple_bytestorage_dimension:fluid_cell_256k",
                                    "simple_bytestorage_dimension:advanced_assembler",
                                    "simple_bytestorage_dimension:singular_assembler",
                                    "simple_bytestorage_dimension:pattern_bus_mk2",
                                    "simple_bytestorage_dimension:singular_controller",
                                    "simple_bytestorage_dimension:interdimensional_energy_transmitter",
                                    "simple_bytestorage_dimension:speed_card_mk2",
                                    "simple_bytestorage_dimension:interdimensional_wireless_card",
                                    "simple_bytestorage_dimension:facade_scanner"
                            )
                    ));
        });
    }

    private void registerDimensionEffects(final RegisterDimensionSpecialEffectsEvent event) {
        event.register(SiliconPlaneDimension.SILICON_PLANE_ID, new SiliconPlaneSkyEffects());
    }
}
