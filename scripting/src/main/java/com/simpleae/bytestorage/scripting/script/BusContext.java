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
}
