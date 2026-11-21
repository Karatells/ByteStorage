package com.simpleae.bytestorage;

import com.simpleae.bytestorage.api.storage.CellTier;
import com.simpleae.bytestorage.block.AssemblerBlock;
import com.simpleae.bytestorage.block.CableBlock;
import com.simpleae.bytestorage.block.PatternProviderBlock;
import com.simpleae.bytestorage.block.CraftingTerminalBlock;
import com.simpleae.bytestorage.block.DriveBlock;
import com.simpleae.bytestorage.block.NetworkControllerBlock;
import com.simpleae.bytestorage.block.PatternEncodingTerminalBlock;
import com.simpleae.bytestorage.block.PatternTerminalBlock;
import com.simpleae.bytestorage.block.TerminalBlock;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.block.entity.CableBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternProviderBlockEntity;
import com.simpleae.bytestorage.block.entity.CraftingTerminalBlockEntity;
import com.simpleae.bytestorage.block.entity.DriveBlockEntity;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternEncodingTerminalBlockEntity;
import com.simpleae.bytestorage.block.entity.PatternTerminalBlockEntity;
import com.simpleae.bytestorage.block.entity.TerminalBlockEntity;
import com.simpleae.bytestorage.client.gui.AssemblerScreen;
import com.simpleae.bytestorage.client.gui.CraftingTerminalScreen;
import com.simpleae.bytestorage.client.gui.DriveScreen;
import com.simpleae.bytestorage.client.gui.PatternEncodingTerminalScreen;
import com.simpleae.bytestorage.client.gui.PatternProviderScreen;
import com.simpleae.bytestorage.client.gui.PatternTerminalScreen;
import com.simpleae.bytestorage.client.gui.TerminalScreen;
import com.simpleae.bytestorage.item.ByteStorageCellItem;
import com.simpleae.bytestorage.item.PatternItem;
import com.simpleae.bytestorage.menu.AssemblerMenu;
import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import com.simpleae.bytestorage.menu.DriveMenu;
import com.simpleae.bytestorage.menu.PatternEncodingTerminalMenu;
import com.simpleae.bytestorage.menu.PatternProviderMenu;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import com.simpleae.bytestorage.menu.TerminalMenu;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Головний клас моду Simple Byte Storage.
 * Реєстрація блоків, предметів, сутностей, контейнерів та пакетів.
 */
@Mod(SimpleByteStorage.MOD_ID)
public class SimpleByteStorage {

    public static final String MOD_ID = "simple_bytestorage";

    // --- Deferred Registers ---
    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // --- 1. Контролер мережі (Network Controller) ---
    public static final RegistryObject<Block> CONTROLLER_BLOCK = BLOCKS.register("controller",
            () -> new NetworkControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CONTROLLER_BLOCK_ITEM = ITEMS.register("controller",
            () -> new BlockItem(CONTROLLER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<NetworkControllerBlockEntity>> CONTROLLER_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("controller",
                    () -> BlockEntityType.Builder.of(NetworkControllerBlockEntity::new, CONTROLLER_BLOCK.get()).build(null));

    // --- 2. Блок накопичувача (Drive Block) ---
    public static final RegistryObject<Block> DRIVE_BLOCK = BLOCKS.register("drive",
            () -> new DriveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> DRIVE_BLOCK_ITEM = ITEMS.register("drive",
            () -> new BlockItem(DRIVE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<DriveBlockEntity>> DRIVE_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("drive",
                    () -> BlockEntityType.Builder.of(DriveBlockEntity::new, DRIVE_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<DriveMenu>> DRIVE_MENU = 
            MENU_TYPES.register("drive", () -> IForgeMenuType.create(DriveMenu::new));

    // --- 3. Блок термінала (Terminal Block) ---
    public static final RegistryObject<Block> TERMINAL_BLOCK = BLOCKS.register("terminal",
            () -> new TerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> TERMINAL_BLOCK_ITEM = ITEMS.register("terminal",
            () -> new BlockItem(TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<TerminalBlockEntity>> TERMINAL_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("terminal",
                    () -> BlockEntityType.Builder.of(TerminalBlockEntity::new, TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<TerminalMenu>> TERMINAL_MENU = 
            MENU_TYPES.register("terminal", () -> IForgeMenuType.create(TerminalMenu::new));

    // --- 4. Блок терміналу крафту (Crafting Terminal Block) ---
    public static final RegistryObject<Block> CRAFTING_TERMINAL_BLOCK = BLOCKS.register("crafting_terminal",
            () -> new CraftingTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CRAFTING_TERMINAL_BLOCK_ITEM = ITEMS.register("crafting_terminal",
            () -> new BlockItem(CRAFTING_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<CraftingTerminalBlockEntity>> CRAFTING_TERMINAL_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("crafting_terminal",
                    () -> BlockEntityType.Builder.of(CraftingTerminalBlockEntity::new, CRAFTING_TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<CraftingTerminalMenu>> CRAFTING_TERMINAL_MENU = 
            MENU_TYPES.register("crafting_terminal", () -> IForgeMenuType.create(CraftingTerminalMenu::new));

    // --- 5. Блок Асемблера (Assembler Block) ---
    public static final RegistryObject<Block> ASSEMBLER_BLOCK = BLOCKS.register("assembler",
            () -> new AssemblerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> ASSEMBLER_BLOCK_ITEM = ITEMS.register("assembler",
            () -> new BlockItem(ASSEMBLER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("assembler",
                    () -> BlockEntityType.Builder.of(AssemblerBlockEntity::new, ASSEMBLER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<AssemblerMenu>> ASSEMBLER_MENU = 
            MENU_TYPES.register("assembler", () -> IForgeMenuType.create(AssemblerMenu::new));

    // --- 6. Блок Терміналу Шаблонів (Pattern Terminal Block) ---
    public static final RegistryObject<Block> PATTERN_TERMINAL_BLOCK = BLOCKS.register("pattern_terminal",
            () -> new PatternTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> PATTERN_TERMINAL_BLOCK_ITEM = ITEMS.register("pattern_terminal",
            () -> new BlockItem(PATTERN_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PatternTerminalBlockEntity>> PATTERN_TERMINAL_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("pattern_terminal",
                    () -> BlockEntityType.Builder.of(PatternTerminalBlockEntity::new, PATTERN_TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<PatternTerminalMenu>> PATTERN_TERMINAL_MENU = 
            MENU_TYPES.register("pattern_terminal", () -> IForgeMenuType.create(PatternTerminalMenu::new));

    // --- 7. Блок Терміналу Кодування Шаблонів (Pattern Encoding Terminal Block) ---
    public static final RegistryObject<Block> PATTERN_ENCODING_TERMINAL_BLOCK = BLOCKS.register("pattern_encoding_terminal",
            () -> new PatternEncodingTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> PATTERN_ENCODING_TERMINAL_BLOCK_ITEM = ITEMS.register("pattern_encoding_terminal",
            () -> new BlockItem(PATTERN_ENCODING_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PatternEncodingTerminalBlockEntity>> PATTERN_ENCODING_TERMINAL_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("pattern_encoding_terminal",
                    () -> BlockEntityType.Builder.of(PatternEncodingTerminalBlockEntity::new, PATTERN_ENCODING_TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<PatternEncodingTerminalMenu>> PATTERN_ENCODING_TERMINAL_MENU = 
            MENU_TYPES.register("pattern_encoding_terminal", () -> IForgeMenuType.create(PatternEncodingTerminalMenu::new));

    // --- 7b. Кабель (Cable) ---
    public static final RegistryObject<Block> CABLE_BLOCK = BLOCKS.register("cable",
            () -> new CableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> CABLE_BLOCK_ITEM = ITEMS.register("cable",
            () -> new BlockItem(CABLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<CableBlockEntity>> CABLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("cable",
                    () -> BlockEntityType.Builder.of(CableBlockEntity::new, CABLE_BLOCK.get()).build(null));

    // --- 7c. Шина Шаблонів (Pattern Provider) ---
    public static final RegistryObject<Block> PATTERN_PROVIDER_BLOCK = BLOCKS.register("pattern_provider",
            () -> new PatternProviderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> PATTERN_PROVIDER_BLOCK_ITEM = ITEMS.register("pattern_provider",
            () -> new BlockItem(PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PatternProviderBlockEntity>> PATTERN_PROVIDER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("pattern_provider",
                    () -> BlockEntityType.Builder.of(PatternProviderBlockEntity::new, PATTERN_PROVIDER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<PatternProviderMenu>> PATTERN_PROVIDER_MENU =
            MENU_TYPES.register("pattern_provider", () -> IForgeMenuType.create(PatternProviderMenu::new));

    // --- 7d. I/O-шина (Import/Export Bus) ---
    public static final RegistryObject<Block> IO_BUS_BLOCK = BLOCKS.register("io_bus",
            () -> new com.simpleae.bytestorage.block.IoBusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> IO_BUS_BLOCK_ITEM = ITEMS.register("io_bus",
            () -> new BlockItem(IO_BUS_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.IoBusBlockEntity>> IO_BUS_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("io_bus",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.IoBusBlockEntity::new, IO_BUS_BLOCK.get()).build(null));

    // --- 7e. Энергоинтерфейс (Energy Interface): заряжает соседние машины энергией сети ---
    public static final RegistryObject<Block> ENERGY_PORT_BLOCK = BLOCKS.register("energy_port",
            () -> new com.simpleae.bytestorage.block.EnergyPortBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> ENERGY_PORT_BLOCK_ITEM = ITEMS.register("energy_port",
            () -> new BlockItem(ENERGY_PORT_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.EnergyPortBlockEntity>> ENERGY_PORT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("energy_port",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.EnergyPortBlockEntity::new, ENERGY_PORT_BLOCK.get()).build(null));

    // --- 7f. Конденсатор (Capacitor): резервный накопитель энергии сети ---
    public static final RegistryObject<Block> CAPACITOR_BLOCK = BLOCKS.register("capacitor",
            () -> new com.simpleae.bytestorage.block.CapacitorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CAPACITOR_BLOCK_ITEM = ITEMS.register("capacitor",
            () -> new BlockItem(CAPACITOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.CapacitorBlockEntity>> CAPACITOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("capacitor",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.CapacitorBlockEntity::new, CAPACITOR_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.IoBusMenu>> IO_BUS_MENU =
            MENU_TYPES.register("io_bus", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.IoBusMenu::new));

    // --- 7g. Шина зберігання (Storage Bus) ---
    public static final RegistryObject<Block> STORAGE_BUS_BLOCK = BLOCKS.register("storage_bus",
            () -> new com.simpleae.bytestorage.block.StorageBusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> STORAGE_BUS_BLOCK_ITEM = ITEMS.register("storage_bus",
            () -> new BlockItem(STORAGE_BUS_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.StorageBusBlockEntity>> STORAGE_BUS_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("storage_bus",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.StorageBusBlockEntity::new, STORAGE_BUS_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.StorageBusMenu>> STORAGE_BUS_MENU =
            MENU_TYPES.register("storage_bus", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.StorageBusMenu::new));

    // --- 7h. Верстак ячеек (Cell Workbench) ---
    public static final RegistryObject<Block> CELL_WORKBENCH_BLOCK = BLOCKS.register("cell_workbench",
            () -> new com.simpleae.bytestorage.block.CellWorkbenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CELL_WORKBENCH_BLOCK_ITEM = ITEMS.register("cell_workbench",
            () -> new BlockItem(CELL_WORKBENCH_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.CellWorkbenchBlockEntity>> CELL_WORKBENCH_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("cell_workbench",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.CellWorkbenchBlockEntity::new, CELL_WORKBENCH_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.CellWorkbenchMenu>> CELL_WORKBENCH_MENU =
            MENU_TYPES.register("cell_workbench", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.CellWorkbenchMenu::new));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.ArchiverMenu>> ARCHIVER_MENU =
            MENU_TYPES.register("archiver", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.ArchiverMenu::new));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.FluidIoBusMenu>> FLUID_IO_BUS_MENU =
            MENU_TYPES.register("fluid_io_bus", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.FluidIoBusMenu::new));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.QuantumRelayMenu>> QUANTUM_RELAY_MENU =
            MENU_TYPES.register("quantum_relay", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.QuantumRelayMenu::new));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.SecurityTerminalMenu>> SECURITY_TERMINAL_MENU =
            MENU_TYPES.register("security_terminal", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.SecurityTerminalMenu::new));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.NetworkDisplayMenu>> NETWORK_DISPLAY_MENU =
            MENU_TYPES.register("network_display", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.NetworkDisplayMenu::new));

    // --- 7o. Мережевий інтерфейс (Network Interface) ---
    public static final RegistryObject<Block> NETWORK_INTERFACE_BLOCK = BLOCKS.register("network_interface",
            () -> new com.simpleae.bytestorage.block.NetworkInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> NETWORK_INTERFACE_BLOCK_ITEM = ITEMS.register("network_interface",
            () -> new BlockItem(NETWORK_INTERFACE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.NetworkInterfaceBlockEntity>> NETWORK_INTERFACE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("network_interface",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.NetworkInterfaceBlockEntity::new, NETWORK_INTERFACE_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.NetworkInterfaceMenu>> NETWORK_INTERFACE_MENU =
            MENU_TYPES.register("network_interface", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.NetworkInterfaceMenu::new));

    // --- 7i. Квантовий ретранслятор (Quantum Relay) ---
    public static final RegistryObject<Block> QUANTUM_RELAY_BLOCK = BLOCKS.register("quantum_relay",
            () -> new com.simpleae.bytestorage.block.QuantumRelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> QUANTUM_RELAY_BLOCK_ITEM = ITEMS.register("quantum_relay",
            () -> new BlockItem(QUANTUM_RELAY_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.QuantumRelayBlockEntity>> QUANTUM_RELAY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("quantum_relay",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.QuantumRelayBlockEntity::new, QUANTUM_RELAY_BLOCK.get()).build(null));

    // --- 7j. Сопроцесор крафту (Crafting Co-Processor) ---
    public static final RegistryObject<Block> CRAFTING_COPROCESSOR_BLOCK = BLOCKS.register("crafting_coprocessor",
            () -> new com.simpleae.bytestorage.block.CraftingCoprocessorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CRAFTING_COPROCESSOR_BLOCK_ITEM = ITEMS.register("crafting_coprocessor",
            () -> new BlockItem(CRAFTING_COPROCESSOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.CraftingCoprocessorBlockEntity>> CRAFTING_COPROCESSOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("crafting_coprocessor",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.CraftingCoprocessorBlockEntity::new, CRAFTING_COPROCESSOR_BLOCK.get()).build(null));

    // --- 7k. Термінал безпеки (Security Terminal) ---
    public static final RegistryObject<Block> SECURITY_TERMINAL_BLOCK = BLOCKS.register("security_terminal",
            () -> new com.simpleae.bytestorage.block.SecurityTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> SECURITY_TERMINAL_BLOCK_ITEM = ITEMS.register("security_terminal",
            () -> new BlockItem(SECURITY_TERMINAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.SecurityTerminalBlockEntity>> SECURITY_TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("security_terminal",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.SecurityTerminalBlockEntity::new, SECURITY_TERMINAL_BLOCK.get()).build(null));

    // --- 7l. Архіватор (Archiver) ---
    public static final RegistryObject<Block> ARCHIVER_BLOCK = BLOCKS.register("archiver",
            () -> new com.simpleae.bytestorage.block.ArchiverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> ARCHIVER_BLOCK_ITEM = ITEMS.register("archiver",
            () -> new BlockItem(ARCHIVER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.ArchiverBlockEntity>> ARCHIVER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("archiver",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.ArchiverBlockEntity::new, ARCHIVER_BLOCK.get()).build(null));

    // --- 7m. Рідинна I/O-шина (Fluid I/O Bus) ---
    public static final RegistryObject<Block> FLUID_IO_BUS_BLOCK = BLOCKS.register("fluid_io_bus",
            () -> new com.simpleae.bytestorage.block.FluidIoBusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> FLUID_IO_BUS_BLOCK_ITEM = ITEMS.register("fluid_io_bus",
            () -> new BlockItem(FLUID_IO_BUS_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.FluidIoBusBlockEntity>> FLUID_IO_BUS_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("fluid_io_bus",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.FluidIoBusBlockEntity::new, FLUID_IO_BUS_BLOCK.get()).build(null));

    // --- 7n. Інформаційний монітор мережі (Network Display) ---
    public static final RegistryObject<Block> NETWORK_DISPLAY_BLOCK = BLOCKS.register("network_display",
            () -> new com.simpleae.bytestorage.block.NetworkDisplayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> NETWORK_DISPLAY_BLOCK_ITEM = ITEMS.register("network_display",
            () -> new BlockItem(NETWORK_DISPLAY_BLOCK.get(), new Item.Properties()));

    // --- 7o. Руда Байтит та ресурси біому «Кремнієвий розлом» ---
    public static final RegistryObject<Block> BYTITE_ORE_BLOCK = BLOCKS.register("bytite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE), net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    public static final RegistryObject<Item> BYTITE_ORE_BLOCK_ITEM = ITEMS.register("bytite_ore",
            () -> new BlockItem(BYTITE_ORE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> DEEPSLATE_BYTITE_ORE_BLOCK = BLOCKS.register("deepslate_bytite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE), net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    public static final RegistryObject<Item> DEEPSLATE_BYTITE_ORE_BLOCK_ITEM = ITEMS.register("deepslate_bytite_ore",
            () -> new BlockItem(DEEPSLATE_BYTITE_ORE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> BYTITE_BLOCK = BLOCKS.register("bytite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> BYTITE_BLOCK_ITEM = ITEMS.register("bytite_block",
            () -> new BlockItem(BYTITE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> SILICATE_CRUST_BLOCK = BLOCKS.register("silicate_crust",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.BASALT)));

    public static final RegistryObject<Item> SILICATE_CRUST_BLOCK_ITEM = ITEMS.register("silicate_crust",
            () -> new BlockItem(SILICATE_CRUST_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> QUANTUM_CLUSTER_BLOCK = BLOCKS.register("quantum_cluster",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(s -> 7)
                    .strength(1.5F)
                    .sound(SoundType.AMETHYST_CLUSTER)));

    public static final RegistryObject<Item> QUANTUM_CLUSTER_BLOCK_ITEM = ITEMS.register("quantum_cluster",
            () -> new BlockItem(QUANTUM_CLUSTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Block> CIRCUIT_BLOOM_BLOCK = BLOCKS.register("circuit_bloom",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .noCollission()
                    .instabreak()
                    .lightLevel(s -> 9)
                    .sound(SoundType.GRASS)));

    public static final RegistryObject<Item> CIRCUIT_BLOOM_BLOCK_ITEM = ITEMS.register("circuit_bloom",
            () -> new BlockItem(CIRCUIT_BLOOM_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<com.simpleae.bytestorage.block.entity.NetworkDisplayBlockEntity>> NETWORK_DISPLAY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("network_display",
                    () -> BlockEntityType.Builder.of(com.simpleae.bytestorage.block.entity.NetworkDisplayBlockEntity::new, NETWORK_DISPLAY_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.ControllerMenu>> CONTROLLER_MENU =
            MENU_TYPES.register("controller", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.ControllerMenu::new));

    // --- 8. Предмет Шаблону Крафту (Pattern Item) ---
    public static final RegistryObject<Item> PATTERN_ITEM = ITEMS.register("pattern",
            () -> new PatternItem(new Item.Properties().stacksTo(1)));

    // --- 8b. Карта прискорення (Speed Card) ---
    public static final RegistryObject<Item> SPEED_CARD = ITEMS.register("speed_card",
            () -> new com.simpleae.bytestorage.item.SpeedCardItem(new Item.Properties().stacksTo(16)));

    // --- 8c. Карти накопичувача та сканер мережі ---
    public static final RegistryObject<Item> VOID_CARD = ITEMS.register("void_card",
            () -> new com.simpleae.bytestorage.item.VoidCardItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> LOCK_CARD = ITEMS.register("lock_card",
            () -> new com.simpleae.bytestorage.item.LockCardItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> NETWORK_ANALYZER = ITEMS.register("network_analyzer",
            () -> new com.simpleae.bytestorage.item.NetworkAnalyzerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENTANGLED_CRYSTAL = ITEMS.register("entangled_crystal",
            () -> new com.simpleae.bytestorage.item.EntangledCrystalItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> DATA_BLOB = ITEMS.register("data_blob",
            () -> new com.simpleae.bytestorage.item.DataBlobItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> FLUID_CELL_1K = ITEMS.register("fluid_cell_1k",
            () -> new com.simpleae.bytestorage.item.FluidCellItem(CellTier.TIER_1K, new Item.Properties()));

    public static final RegistryObject<Item> FLUID_CELL_4K = ITEMS.register("fluid_cell_4k",
            () -> new com.simpleae.bytestorage.item.FluidCellItem(CellTier.TIER_4K, new Item.Properties()));

    public static final RegistryObject<Item> FLUID_CELL_16K = ITEMS.register("fluid_cell_16k",
            () -> new com.simpleae.bytestorage.item.FluidCellItem(CellTier.TIER_16K, new Item.Properties()));

    public static final RegistryObject<Item> FLUID_CELL_64K = ITEMS.register("fluid_cell_64k",
            () -> new com.simpleae.bytestorage.item.FluidCellItem(CellTier.TIER_64K, new Item.Properties()));

    // --- 8d. Базові та біомні матеріали ---
    public static final RegistryObject<Item> RAW_BYTITE = ITEMS.register("raw_bytite",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BYTITE_INGOT = ITEMS.register("bytite_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SILICON_DUST = ITEMS.register("silicon_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LOGIC_WAFER = ITEMS.register("logic_wafer",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> QUANTUM_QUARTZ = ITEMS.register("quantum_quartz",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CYAN_PHOSPHOR = ITEMS.register("cyan_phosphor",
            () -> new Item(new Item.Properties()));

    // --- 8c. Бездротовий термінал ---
    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new com.simpleae.bytestorage.item.GuideBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WIRELESS_TERMINAL = ITEMS.register("wireless_terminal",
            () -> new com.simpleae.bytestorage.item.WirelessTerminalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> AUTO_TRANSFER_CARD = ITEMS.register("auto_transfer_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.AUTO_TRANSFER, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INSURANCE_CARD = ITEMS.register("insurance_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.INSURANCE, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> TOOL_CARD = ITEMS.register("tool_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.TOOL, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MACRO_CARD = ITEMS.register("macro_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.MACRO, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MAGNET_CARD = ITEMS.register("magnet_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.MAGNET, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CRAFTING_CARD = ITEMS.register("crafting_card",
            () -> new com.simpleae.bytestorage.item.WirelessUpgradeItem(com.simpleae.bytestorage.item.WirelessUpgradeItem.Type.CRAFTING, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> RECIPE_LIBRARY = ITEMS.register("recipe_library",
            () -> new com.simpleae.bytestorage.item.RecipeLibraryItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.WirelessSettingsMenu>> WIRELESS_SETTINGS_MENU =
            MENU_TYPES.register("wireless_settings", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.WirelessSettingsMenu::new));

    // --- 9. Диски зберігання (Storage Cells: 1k, 4k, 16k, 64k) ---
    public static final RegistryObject<Item> CELL_1K = ITEMS.register("cell_1k",
            () -> new ByteStorageCellItem(CellTier.TIER_1K, new Item.Properties()));

    public static final RegistryObject<Item> CELL_4K = ITEMS.register("cell_4k",
            () -> new ByteStorageCellItem(CellTier.TIER_4K, new Item.Properties()));

    public static final RegistryObject<Item> CELL_16K = ITEMS.register("cell_16k",
            () -> new ByteStorageCellItem(CellTier.TIER_16K, new Item.Properties()));

    public static final RegistryObject<Item> CELL_64K = ITEMS.register("cell_64k",
            () -> new ByteStorageCellItem(CellTier.TIER_64K, new Item.Properties()));

    public static final RegistryObject<Item> CELL_256K = ITEMS.register("cell_256k",
            () -> new ByteStorageCellItem(CellTier.TIER_256K, new Item.Properties()));

    public static final RegistryObject<Item> CELL_1M = ITEMS.register("cell_1m",
            () -> new ByteStorageCellItem(CellTier.TIER_1M, new Item.Properties()));

    // --- 6. Креативна вкладка ---
    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("simple_bytestorage_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_bytestorage"))
                    .icon(() -> new ItemStack(CONTROLLER_BLOCK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(CONTROLLER_BLOCK.get());
                        output.accept(DRIVE_BLOCK.get());
                        output.accept(TERMINAL_BLOCK.get());
                        output.accept(CRAFTING_TERMINAL_BLOCK.get());
                        output.accept(ASSEMBLER_BLOCK.get());
                        output.accept(PATTERN_TERMINAL_BLOCK.get());
                        output.accept(PATTERN_ENCODING_TERMINAL_BLOCK.get());
                        output.accept(CABLE_BLOCK.get());
                        output.accept(PATTERN_PROVIDER_BLOCK.get());
                        output.accept(IO_BUS_BLOCK.get());
                        output.accept(STORAGE_BUS_BLOCK.get());
                        output.accept(CELL_WORKBENCH_BLOCK.get());
                        output.accept(QUANTUM_RELAY_BLOCK.get());
                        output.accept(CRAFTING_COPROCESSOR_BLOCK.get());
                        output.accept(SECURITY_TERMINAL_BLOCK.get());
                        output.accept(ARCHIVER_BLOCK.get());
                        output.accept(FLUID_IO_BUS_BLOCK.get());
                        output.accept(NETWORK_DISPLAY_BLOCK.get());
                        output.accept(NETWORK_INTERFACE_BLOCK.get());
                        output.accept(ENERGY_PORT_BLOCK.get());
                        output.accept(CAPACITOR_BLOCK.get());
                        output.accept(PATTERN_ITEM.get());
                        output.accept(SPEED_CARD.get());
                        output.accept(VOID_CARD.get());
                        output.accept(LOCK_CARD.get());
                        output.accept(NETWORK_ANALYZER.get());
                        output.accept(ENTANGLED_CRYSTAL.get());
                        output.accept(WIRELESS_TERMINAL.get());
                        output.accept(AUTO_TRANSFER_CARD.get());
                        output.accept(INSURANCE_CARD.get());
                        output.accept(TOOL_CARD.get());
                        output.accept(MACRO_CARD.get());
                        output.accept(MAGNET_CARD.get());
                        output.accept(CRAFTING_CARD.get());
                        output.accept(RECIPE_LIBRARY.get());
                        output.accept(GUIDE_BOOK.get());
                        output.accept(CELL_1K.get());
                        output.accept(CELL_4K.get());
                        output.accept(CELL_16K.get());
                        output.accept(CELL_64K.get());
                        output.accept(CELL_256K.get());
                        output.accept(CELL_1M.get());
                        output.accept(FLUID_CELL_1K.get());
                        output.accept(FLUID_CELL_4K.get());
                        output.accept(FLUID_CELL_16K.get());
                        output.accept(FLUID_CELL_64K.get());
                        output.accept(BYTITE_ORE_BLOCK.get());
                        output.accept(DEEPSLATE_BYTITE_ORE_BLOCK.get());
                        output.accept(BYTITE_BLOCK.get());
                        output.accept(SILICATE_CRUST_BLOCK.get());
                        output.accept(QUANTUM_CLUSTER_BLOCK.get());
                        output.accept(CIRCUIT_BLOOM_BLOCK.get());
                        output.accept(RAW_BYTITE.get());
                        output.accept(BYTITE_INGOT.get());
                        output.accept(SILICON_DUST.get());
                        output.accept(LOGIC_WAFER.get());
                        output.accept(QUANTUM_QUARTZ.get());
                        output.accept(CYAN_PHOSPHOR.get());
                    })
                    .build());

    public SimpleByteStorage() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SimpleByteStoragePackets::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.simpleae.bytestorage.client.guide.GuideRegistry.registerBuiltin();
            MenuScreens.register(TERMINAL_MENU.get(), TerminalScreen::new);
            MenuScreens.register(CRAFTING_TERMINAL_MENU.get(), CraftingTerminalScreen::new);
            MenuScreens.register(DRIVE_MENU.get(), DriveScreen::new);
            MenuScreens.register(ASSEMBLER_MENU.get(), AssemblerScreen::new);
            MenuScreens.register(PATTERN_TERMINAL_MENU.get(), PatternTerminalScreen::new);
            MenuScreens.register(PATTERN_ENCODING_TERMINAL_MENU.get(), PatternEncodingTerminalScreen::new);
            MenuScreens.register(PATTERN_PROVIDER_MENU.get(), PatternProviderScreen::new);
            MenuScreens.register(IO_BUS_MENU.get(), com.simpleae.bytestorage.client.gui.IoBusScreen::new);
            MenuScreens.register(STORAGE_BUS_MENU.get(), com.simpleae.bytestorage.client.gui.StorageBusScreen::new);
            MenuScreens.register(CELL_WORKBENCH_MENU.get(), com.simpleae.bytestorage.client.gui.CellWorkbenchScreen::new);
            MenuScreens.register(ARCHIVER_MENU.get(), com.simpleae.bytestorage.client.gui.ArchiverScreen::new);
            MenuScreens.register(FLUID_IO_BUS_MENU.get(), com.simpleae.bytestorage.client.gui.FluidIoBusScreen::new);
            MenuScreens.register(QUANTUM_RELAY_MENU.get(), com.simpleae.bytestorage.client.gui.QuantumRelayScreen::new);
            MenuScreens.register(SECURITY_TERMINAL_MENU.get(), com.simpleae.bytestorage.client.gui.SecurityTerminalScreen::new);
            MenuScreens.register(NETWORK_DISPLAY_MENU.get(), com.simpleae.bytestorage.client.gui.NetworkDisplayScreen::new);
            MenuScreens.register(NETWORK_INTERFACE_MENU.get(), com.simpleae.bytestorage.client.gui.NetworkInterfaceScreen::new);
            MenuScreens.register(WIRELESS_SETTINGS_MENU.get(), com.simpleae.bytestorage.client.gui.WirelessSettingsScreen::new);
            MenuScreens.register(CONTROLLER_MENU.get(), com.simpleae.bytestorage.client.gui.ControllerScreen::new);
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                    NETWORK_DISPLAY_BLOCK_ENTITY.get(),
                    com.simpleae.bytestorage.client.render.NetworkDisplayRenderer::new);
        });
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onBlockPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide) {
            notifyGridTopology(level, event.getPos());
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide) {
            notifyGridTopology(level, event.getPos());
        }
    }

    private static void notifyGridTopology(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            net.minecraft.core.BlockPos adj = pos.relative(dir);
            if (!level.isLoaded(adj)) continue;
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(adj);
            if (be instanceof com.simpleae.bytestorage.api.network.IGridNode node
                    && node.getGrid() != null && node.getGrid().getController() != null) {
                node.getGrid().getController().markTopologyDirty();
            }
        }
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof com.simpleae.bytestorage.api.network.IGridNode self
                && self.getGrid() != null && self.getGrid().getController() != null) {
            self.getGrid().getController().markTopologyDirty();
        }
    }
}
