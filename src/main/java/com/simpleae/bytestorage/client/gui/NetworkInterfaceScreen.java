package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.NetworkInterfaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Экран Сетевого интерфейса: конфигурация поддерживаемого запаса (9) и буфер (9). */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    public NetworkInterfaceScreen(NetworkInterfaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = 87;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int code = this.menu.getStatusCode();
        int led = code == 1 ? GuiStyle.LED_GOOD : (code == 2 ? GuiStyle.LED_WARN : GuiStyle.LED_BAD);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "arrows");

        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.interface.config"), x + 8, y + 20);
        for (int i = NetworkInterfaceMenu.CONFIG_START; i < NetworkInterfaceMenu.CONFIG_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_CARD);
        }

        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.interface.buffer"), x + 8, y + 52);
        for (int i = NetworkInterfaceMenu.BUFFER_START; i < NetworkInterfaceMenu.BUFFER_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, NetworkInterfaceMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
