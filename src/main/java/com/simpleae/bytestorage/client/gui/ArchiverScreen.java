package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.ArchiverMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Байт-архіватора у стилі мода. */
public class ArchiverScreen extends AbstractContainerScreen<ArchiverMenu> {

    private UiButton toggleButton;

    public ArchiverScreen(ArchiverMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.toggleButton = new UiButton(this.leftPos + 8, this.topPos + 51, 78, 16,
                () -> Component.translatable(this.menu.isEnabled()
                        ? "gui.simple_bytestorage.archiver.active" : "gui.simple_bytestorage.archiver.paused"),
                UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, ArchiverMenu.BUTTON_TOGGLE);
            }
        }).primary();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.toggleButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.toggleButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.isEcoMode() ? GuiStyle.LED_WARN : GuiStyle.LED_GOOD);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "chip");

        GuiStyle.display(graphics, x + 6, y + 20, 164, 24);
        String info = Component.translatable("gui.simple_bytestorage.archiver.info").getString();
        graphics.drawString(this.font, GuiStyle.clip(this.font, info, 156),
                x + 10, y + 28, GuiStyle.TEXT_LABEL, false);

        GuiStyle.display(graphics, x + 6, y + 48, 164, 22);
        Component status = Component.translatable(!menu.isOnline()
                ? "gui.simple_bytestorage.provider.offline"
                : (menu.isEcoMode() ? "gui.simple_bytestorage.eco_mode" : "gui.simple_bytestorage.storage_bus.ok"));
        int color = !menu.isOnline() ? GuiStyle.TEXT_BAD : (menu.isEcoMode() ? GuiStyle.TEXT_WARN : GuiStyle.TEXT_GOOD);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), 78), x + 90, y + 55, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, 0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
