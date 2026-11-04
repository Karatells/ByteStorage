package com.simpleae.bytestorage.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.WirelessMacroPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Горячая клавиша запуска макроса Беспроводного терминала. */
public final class WirelessKeys {

    public static final KeyMapping MACRO = new KeyMapping("key.simple_bytestorage.wireless_macro",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.simple_bytestorage");

    private WirelessKeys() {
    }

    @Mod.EventBusSubscriber(modid = SimpleByteStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(MACRO);
        }
    }

    @Mod.EventBusSubscriber(modid = SimpleByteStorage.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) {
                return;
            }
            while (MACRO.consumeClick()) {
                if (Minecraft.getInstance().screen == null) {
                    SimpleByteStoragePackets.sendToServer(new WirelessMacroPacket());
                }
            }
        }
    }
}
