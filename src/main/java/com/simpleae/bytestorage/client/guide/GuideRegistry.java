package com.simpleae.bytestorage.client.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Реєстр розділів Книги-довідника. Основний мод реєструє свої розділи з іконками та рецептами,
 * а аддони (наприклад, Scripting) додають власні через {@link #register} у клієнтському setup.
 */
public final class GuideRegistry {

    /**
     * Розділ книги. Чим менше {@code order}, тим вище в списку.
     * {@code iconId} — ID предмета/блоку для 3D-іконки розділу.
     * {@code recipeItems} — список ID предметів/блоків, рецепти яких показуються на вкладці «Крафти».
     */
    public record Chapter(String id, int order, String titleKey, String bodyKey,
                          String iconId, List<String> recipeItems) {

        public Chapter(String id, int order, String titleKey, String bodyKey) {
            this(id, order, titleKey, bodyKey, "simple_bytestorage:guide_book", List.of());
        }
    }

    private static final List<Chapter> CHAPTERS = new ArrayList<>();

    private GuideRegistry() {
    }

    /** Додає розділ (повторна реєстрація з тим самим id замінює попередній). */
    public static synchronized void register(Chapter chapter) {
        CHAPTERS.removeIf(c -> c.id().equals(chapter.id()));
        CHAPTERS.add(chapter);
    }

    public static synchronized List<Chapter> chapters() {
        List<Chapter> copy = new ArrayList<>(CHAPTERS);
        copy.sort(Comparator.comparingInt(Chapter::order));
        return copy;
    }

    /** Розділи основного мода з іконками та крафтами. */
    public static void registerBuiltin() {
        reg("intro", 10, "simple_bytestorage:guide_book", List.of(
                "simple_bytestorage:guide_book",
                "simple_bytestorage:bytite_ingot",
                "simple_bytestorage:logic_wafer",
                "simple_bytestorage:cyan_phosphor"
        ));
        reg("network", 20, "simple_bytestorage:cable", List.of(
                "simple_bytestorage:controller",
                "simple_bytestorage:cable",
                "simple_bytestorage:cable_facade",
                "simple_bytestorage:energy_port",
                "simple_bytestorage:capacitor"
        ));
        reg("controller", 30, "simple_bytestorage:controller", List.of(
                "simple_bytestorage:controller",
                "simple_bytestorage:recipe_library"
        ));
        reg("storage", 40, "simple_bytestorage:drive", List.of(
                "simple_bytestorage:drive",
                "simple_bytestorage:cell_workbench",
                "simple_bytestorage:storage_bus",
                "simple_bytestorage:archiver",
                "simple_bytestorage:cell_1k",
                "simple_bytestorage:cell_4k",
                "simple_bytestorage:cell_16k",
                "simple_bytestorage:cell_64k",
                "simple_bytestorage:cell_256k",
                "simple_bytestorage:cell_1m",
                "simple_bytestorage:fluid_cell_1k",
                "simple_bytestorage:fluid_cell_4k",
                "simple_bytestorage:fluid_cell_16k",
                "simple_bytestorage:fluid_cell_64k",
                "simple_bytestorage:void_card",
                "simple_bytestorage:lock_card"
        ));
        reg("terminals", 50, "simple_bytestorage:terminal", List.of(
                "simple_bytestorage:terminal",
                "simple_bytestorage:crafting_terminal"
        ));
        reg("wireless", 60, "simple_bytestorage:wireless_terminal", List.of(
                "simple_bytestorage:wireless_terminal",
                "simple_bytestorage:auto_transfer_card",
                "simple_bytestorage:insurance_card",
                "simple_bytestorage:tool_card",
                "simple_bytestorage:macro_card",
                "simple_bytestorage:magnet_card",
                "simple_bytestorage:crafting_card"
        ));
        reg("interface", 65, "simple_bytestorage:network_interface", List.of(
                "simple_bytestorage:network_interface",
                "simple_bytestorage:portable_cell_1k",
                "simple_bytestorage:portable_cell_4k",
                "simple_bytestorage:portable_cell_16k",
                "simple_bytestorage:energy_cell_1k",
                "simple_bytestorage:energy_cell_4k",
                "simple_bytestorage:energy_cell_16k"
        ));
        reg("advanced", 68, "simple_bytestorage:quantum_relay", List.of(
                "simple_bytestorage:quantum_relay",
                "simple_bytestorage:entangled_crystal",
                "simple_bytestorage:network_display",
                "simple_bytestorage:security_terminal",
                "simple_bytestorage:network_analyzer",
                "simple_bytestorage:fluid_io_bus",
                "simple_bytestorage:crafting_coprocessor"
        ));
        reg("autocraft", 70, "simple_bytestorage:pattern_terminal", List.of(
                "simple_bytestorage:pattern_terminal",
                "simple_bytestorage:pattern_encoding_terminal",
                "simple_bytestorage:pattern"
        ));
        reg("assembler", 80, "simple_bytestorage:assembler", List.of(
                "simple_bytestorage:assembler",
                "simple_bytestorage:crafting_coprocessor"
        ));
        reg("provider", 90, "simple_bytestorage:pattern_provider", List.of(
                "simple_bytestorage:pattern_provider"
        ));
        reg("iobus", 95, "simple_bytestorage:io_bus", List.of(
                "simple_bytestorage:io_bus",
                "simple_bytestorage:fluid_io_bus",
                "simple_bytestorage:speed_card"
        ));
        reg("jei", 98, "minecraft:crafting_table", List.of());
    }

    private static void reg(String id, int order, String iconId, List<String> recipeItems) {
        register(new Chapter(id, order,
                "guide.simple_bytestorage." + id + ".title",
                "guide.simple_bytestorage." + id + ".body",
                iconId, recipeItems));
    }
}
