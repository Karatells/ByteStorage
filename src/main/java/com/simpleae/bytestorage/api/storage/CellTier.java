package com.simpleae.bytestorage.api.storage;

/**
 * Рівні (Тіри) дисків зберігання Simple Byte Storage.
 * 
 * У нашому моді діє правило: 1 предмет = 1 байт.
 * Немає обмежень на кількість унікальних типів предметів.
 */
public enum CellTier {
    TIER_1K("1k", 1024, 0x4CAF50, 1),       // 1,024 предметів (Зелений)
    TIER_4K("4k", 4096, 0x2196F3, 2),       // 4,096 предметів (Синій)
    TIER_16K("16k", 16384, 0x9C27B0, 3),    // 16,384 предметів (Фіолетовий)
    TIER_64K("64k", 65536, 0xFF9800, 4);    // 65,536 предметів (Помаранчевий)

    private final String name;
    private final int capacityBytes;
    private final int colorRgb;
    private final int idleEnergyDrain; // FE/t пасивне споживання в накопичувачі

    CellTier(String name, int capacityBytes, int colorRgb, int idleEnergyDrain) {
        this.name = name;
        this.capacityBytes = capacityBytes;
        this.colorRgb = colorRgb;
        this.idleEnergyDrain = idleEnergyDrain;
    }

    public String getName() {
        return name;
    }

    /**
     * Повна ємність у байтах (де 1 байт = 1 предмет).
     */
    public int getCapacityBytes() {
        return capacityBytes;
    }

    public int getColorRgb() {
        return colorRgb;
    }

    public int getIdleEnergyDrain() {
        return idleEnergyDrain;
    }
}
