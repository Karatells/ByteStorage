package com.simpleae.bytestorage.scripting;

import com.simpleae.bytestorage.scripting.block.LogicBusBlock;
import com.simpleae.bytestorage.scripting.block.LogicTerminalBlock;
import com.simpleae.bytestorage.scripting.block.entity.LogicBusBlockEntity;
import com.simpleae.bytestorage.scripting.block.entity.LogicTerminalBlockEntity;
import com.simpleae.bytestorage.scripting.net.ScriptingPackets;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * SimpleAE: Scripting — необов'язковий аддон до SimpleAE: ByteStorage.
 * Без цього мода Логічної шини та Логічного терміналу у грі немає.
 */
@Mod(SimpleAeScripting.MOD_ID)
public class SimpleAeScripting {

    public static final String MOD_ID = "simple_bytestorage_scripting";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Block> LOGIC_BUS_BLOCK = BLOCKS.register("logic_bus",
            () -> new LogicBusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Item> LOGIC_BUS_ITEM = ITEMS.register("logic_bus",
            () -> new BlockItem(LOGIC_BUS_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LogicBusBlockEntity>> LOGIC_BUS_ENTITY =
            BLOCK_ENTITIES.register("logic_bus",
                    () -> BlockEntityType.Builder.of(LogicBusBlockEntity::new, LOGIC_BUS_BLOCK.get()).build(null));

    public static final RegistryObject<Block> LOGIC_TERMINAL_BLOCK = BLOCKS.register("logic_terminal",
            () -> new LogicTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Item> LOGIC_TERMINAL_ITEM = ITEMS.register("logic_terminal",
            () -> new BlockItem(LOGIC_TERMINAL_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LogicTerminalBlockEntity>> LOGIC_TERMINAL_ENTITY =
            BLOCK_ENTITIES.register("logic_terminal",
                    () -> BlockEntityType.Builder.of(LogicTerminalBlockEntity::new, LOGIC_TERMINAL_BLOCK.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("simple_bytestorage_scripting_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simple_bytestorage_scripting"))
                    .icon(() -> new ItemStack(LOGIC_TERMINAL_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(LOGIC_BUS_ITEM.get());
                        output.accept(LOGIC_TERMINAL_ITEM.get());
                    })
                    .build());

    public SimpleAeScripting() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ScriptingConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }
    }

    /** Додає розділи про скрипти в Книгу-довідник основного мода. */
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            String[] ids = {"intro", "api", "examples", "admin"};
            int order = 100;
            for (String id : ids) {
                com.simpleae.bytestorage.client.guide.GuideRegistry.register(
                        new com.simpleae.bytestorage.client.guide.GuideRegistry.Chapter("scripting_" + id, order += 10,
                                "guide.simple_bytestorage_scripting." + id + ".title",
                                "guide.simple_bytestorage_scripting." + id + ".body"));
            }
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ScriptingPackets::register);
    }
}
