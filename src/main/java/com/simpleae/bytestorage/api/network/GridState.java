package com.simpleae.bytestorage.api.network;

import net.minecraft.util.StringRepresentable;

/**
 * Стан енергетичної мережі / Контролера.
 */
public enum GridState implements StringRepresentable {
    OFFLINE("offline"),    // Немає живлення або мережа вимкнена
    ONLINE("online"),      // Мережа активна і має достатньо енергії
    CONFLICT("conflict");  // Конфлікт (наприклад, два контролери в одній замкненій мережі)

    private final String name;

    GridState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isActive() {
        return this == ONLINE;
    }
}
