package com.simpleae.bytestorage.util;

/** Расписание правил запаса: когда сеть вообще делает автозаказы (чистая логика, покрыта тестами). */
public final class StockSchedule {

    public static final int ALWAYS = 0;
    public static final int NIGHT = 1;   // только ночью
    public static final int DAY = 2;     // только днём
    public static final int ENERGY = 3;  // только при достаточном заряде сети
    public static final int MODES = 4;

    /** Возобновить при заряде не ниже, приостановить при падении ниже (гистерезис, чтобы правила не мигали). */
    public static final int ENERGY_RESUME = 50;
    public static final int ENERGY_PAUSE = 25;

    private static final String[] NAMES = {"always", "night", "day", "energy"};

    private StockSchedule() {
    }

    public static boolean isActive(int mode, long dayTime, int energyPercent, boolean wasActive) {
        long t = Math.floorMod(dayTime, 24000L);
        boolean night = t >= 13000 && t < 23000;
        return switch (mode) {
            case NIGHT -> night;
            case DAY -> !night;
            case ENERGY -> wasActive ? energyPercent >= ENERGY_PAUSE : energyPercent >= ENERGY_RESUME;
            default -> true;
        };
    }

    public static String name(int mode) {
        return NAMES[Math.max(0, Math.min(NAMES.length - 1, mode))];
    }

    /** Режим по имени или -1. */
    public static int byName(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
