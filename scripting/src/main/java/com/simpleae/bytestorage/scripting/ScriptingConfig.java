package com.simpleae.bytestorage.scripting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Серверні налаштування аддона (файл config/simple_bytestorage_scripting-common.toml).
 * Адміністратор може вимкнути скрипти повністю, обмежити редагування операторами та задати ліміти.
 */
public final class ScriptingConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue OPS_ONLY;
    public static final ForgeConfigSpec.BooleanValue ALLOW_CRAFTING;
    public static final ForgeConfigSpec.IntValue MAX_SCRIPT_LENGTH;
    public static final ForgeConfigSpec.IntValue MIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue MAX_INSTRUCTIONS;
    public static final ForgeConfigSpec.IntValue MAX_MILLIS;
    public static final ForgeConfigSpec.IntValue MAX_ITEMS_PER_RUN;
    public static final ForgeConfigSpec.IntValue MAX_CALLS_PER_RUN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("scripting");
        ENABLED = b.comment("Головний вимикач. false = усі Логічні шини зупинені (блоки лишаються в світі).")
                .define("enabled", true);
        OPS_ONLY = b.comment("true = змінювати скрипти та запускати/зупиняти шини можуть лише оператори (рівень 2+)",
                        "та власник локального світу. Рекомендовано для публічних серверів.")
                .define("opsOnly", false);
        ALLOW_CRAFTING = b.comment("Дозволити скриптам замовляти автокрафт (net.craft).")
                .define("allowCrafting", true);
        MAX_SCRIPT_LENGTH = b.comment("Максимальна довжина скрипта в символах.")
                .defineInRange("maxScriptLength", 8000, 100, 30000);
        MIN_INTERVAL = b.comment("Мінімальний інтервал між запусками tick() у тіках.")
                .defineInRange("minInterval", 10, 1, 1200);
        MAX_INSTRUCTIONS = b.comment("Ліміт інструкцій JavaScript за один запуск (захист від нескінченних циклів).")
                .defineInRange("maxInstructionsPerRun", 50000, 1000, 5000000);
        MAX_MILLIS = b.comment("Ліміт реального часу одного запуску, мілісекунд.")
                .defineInRange("maxMillisPerRun", 20, 1, 500);
        MAX_ITEMS_PER_RUN = b.comment("Скільки предметів скрипт може перемістити за один запуск.")
                .defineInRange("maxItemsPerRun", 256, 1, 100000);
        MAX_CALLS_PER_RUN = b.comment("Скільки викликів API (net/side/endpoint) дозволено за один запуск.")
                .defineInRange("maxApiCallsPerRun", 200, 10, 10000);
        b.pop();
        SPEC = b.build();
    }

    private ScriptingConfig() {
    }

    /** Чи може гравець змінювати скрипти (з урахуванням opsOnly). */
    public static boolean canEdit(ServerPlayer player) {
        if (!ENABLED.get()) {
            return false;
        }
        if (!OPS_ONLY.get()) {
            return true;
        }
        return player.hasPermissions(2)
                || player.server.isSingleplayerOwner(player.getGameProfile());
    }
}
