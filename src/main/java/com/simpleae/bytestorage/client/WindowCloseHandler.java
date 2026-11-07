package com.simpleae.bytestorage.client;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.client.gui.GuiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Клик по крестику в шапке окна мода закрывает экран (ESC работает как и раньше). */
@Mod.EventBusSubscriber(modid = SimpleByteStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WindowCloseHandler {

    private WindowCloseHandler() {
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (event.getButton() != 0 || !screen.getClass().getName().startsWith("com.simpleae.bytestorage")) {
            return;
        }
        int[] area = GuiStyle.closeButtonArea();
        double mx = event.getMouseX();
        double my = event.getMouseY();
        if (area != null && mx >= area[0] && mx < area[0] + area[2] && my >= area[1] && my < area[1] + area[3]) {
            event.setCanceled(true);
            Minecraft.getInstance().screen.onClose();
        }
    }
}
