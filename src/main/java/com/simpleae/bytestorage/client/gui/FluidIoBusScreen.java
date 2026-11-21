package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.FluidIoBusMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Рідинної I/O-шини у стилі мода. */
public class FluidIoBusScreen extends AbstractContainerScreen<FluidIoBusMenu> {

    private UiButton modeButton;

    public FluidIoBusScreen(FluidIoBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.modeButton = new UiButton(this.leftPos + 8, this.topPos + 51, 88, 16,
                () -> Component.translatable(this.menu.isExportMode()
                        ? "gui.simple_bytestorage.iobus.export" : "gui.simple_bytestorage.iobus.import"),
                UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, FluidIoBusMenu.BUTTON_MODE);
            }
        }).primary().icon("arrows");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modeButton.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.modeButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.isEcoMode() ? GuiStyle.LED_WARN : GuiStyle.LED_GOOD);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "cable");

        GuiStyle.display(graphics, x + 6, y + 20, 164, 24);
        graphics.drawString(this.font, Component.translatable("gui.simple_bytestorage.fluid_bus.info"),
                x + 10, y + 28, GuiStyle.TEXT_LABEL, false);

        GuiStyle.display(graphics, x + 6, y + 48, 164, 22);
        Component status = Component.translatable(!menu.isOnline()
                ? "gui.simple_bytestorage.provider.offline"
                : (menu.isEcoMode() ? "gui.simple_bytestorage.eco_mode" : "gui.simple_bytestorage.storage_bus.ok"));
        int color = !menu.isOnline() ? GuiStyle.TEXT_BAD : (menu.isEcoMode() ? GuiStyle.TEXT_WARN : GuiStyle.TEXT_GOOD);
        graphics.drawString(this.font, GuiStyle.clip(this.font, status.getString(), 68), x + 100, y + 55, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, 0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
