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

    public static final RegistryObject<MenuType<com.simpleae.bytestorage.menu.ControllerMenu>> CONTROLLER_MENU =
            MENU_TYPES.register("controller", () -> IForgeMenuType.create(com.simpleae.bytestorage.menu.ControllerMenu::new));

    // --- 8. Предмет Шаблону Крафту (Pattern Item) ---
    public static final RegistryObject<Item> PATTERN_ITEM = ITEMS.register("pattern",
            () -> new PatternItem(new Item.Properties().stacksTo(1)));

    // --- 8b. Карта прискорення (Speed Card) ---
    public static final RegistryObject<Item> SPEED_CARD = ITEMS.register("speed_card",
            () -> new com.simpleae.bytestorage.item.SpeedCardItem(new Item.Properties().stacksTo(16)));

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
                        output.accept(ENERGY_PORT_BLOCK.get());
                        output.accept(CAPACITOR_BLOCK.get());
                        output.accept(PATTERN_ITEM.get());
                        output.accept(SPEED_CARD.get());
                        output.accept(WIRELESS_TERMINAL.get());
                        output.accept(AUTO_TRANSFER_CARD.get());
                        output.accept(INSURANCE_CARD.get());
                        output.accept(TOOL_CARD.get());
                        output.accept(MACRO_CARD.get());
                        output.accept(RECIPE_LIBRARY.get());
                        output.accept(GUIDE_BOOK.get());
                        output.accept(CELL_1K.get());
                        output.accept(CELL_4K.get());
                        output.accept(CELL_16K.get());
                        output.accept(CELL_64K.get());
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
            MenuScreens.register(WIRELESS_SETTINGS_MENU.get(), com.simpleae.bytestorage.client.gui.WirelessSettingsScreen::new);
            MenuScreens.register(CONTROLLER_MENU.get(), com.simpleae.bytestorage.client.gui.ControllerScreen::new);
        });
    }
}
