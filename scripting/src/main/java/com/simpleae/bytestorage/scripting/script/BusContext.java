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
}
