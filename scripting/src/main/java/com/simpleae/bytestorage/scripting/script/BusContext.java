package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.api.network.IGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Що рантайм знає про шину, на якій виконується скрипт. */
public interface BusContext {
    IGrid grid();

    Level level();

    BlockPos pos();

    void log(String message);

    /** Ідентифікатор цієї шини ("" — не заданий). */
    String identifier();

    /** Усі Логічні шини цієї мережі з таким ідентифікатором (у стабільному порядку за позицією). */
    java.util.List<BusContext> busesWithId(String id);

    /** Заменяет панель, нарисованную скриптом (показывается в Логическом терминале). */
    void setPanel(java.util.List<UiElement> elements);

    /** Забирает нажатые на панели кнопки (идентификаторы) с прошлого запуска. */
    java.util.List<String> takeButtons();

    /** Персистентне сховище ключ-значення шини (зберігається в NBT). */
    default java.util.Map<String, String> memoryStore() {
        return null;
    }

    default void markStoreDirty() {
    }

    /** Повідомлення від іншої Логічної шини через {@code bus.send(targetId, text)}. */
    record BusMessage(String from, String text) {
    }

    default void pushMessage(String from, String text) {
    }

    default java.util.List<BusMessage> takeMessages() {
        return java.util.List.of();
    }

    default int getRedstone(net.minecraft.core.Direction dir) {
        return 0;
    }

    default void setRedstone(net.minecraft.core.Direction dir, int power) {
    }
}
