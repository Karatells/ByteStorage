package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.IoBusMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран I/O-шини: режим імпорт/експорт і список предметів (кількості — для експорту). */
public class IoBusScreen extends AbstractContainerScreen<IoBusMenu> {

    private UiButton modeButton;

    public IoBusScreen(IoBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.modeButton = new UiButton(this.leftPos + 8, this.topPos + 51, 78, 16,
                () -> Component.translatable(this.menu.isExportMode()
                        ? "gui.simple_bytestorage.iobus.export" : "gui.simple_bytestorage.iobus.import"),
                UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, IoBusMenu.BUTTON_MODE);
            }
        }).primary();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.modeButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.modeButton.isOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, Component.translatable(this.menu.isExportMode()
                    ? "gui.simple_bytestorage.iobus.export.tip" : "gui.simple_bytestorage.iobus.import.tip"),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.getMachines() > 0 ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led);

        GuiStyle.panel(graphics, x + 6, y + 20, 164, 24);
        for (int i = 0; i < IoBusMenu.FILTER_SLOTS; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.display(graphics, x + 6, y + 48, 164, 22);
        Component status = Component.translatable(!menu.isOnline()
                ? "gui.simple_bytestorage.provider.offline"
                : (menu.getMachines() > 0 ? "gui.simple_bytestorage.provider.machine.ok"
                : "gui.simple_bytestorage.provider.machine.none"));
        int color = !menu.isOnline() ? GuiStyle.TEXT_BAD : (menu.getMachines() > 0 ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), 78), x + 90, y + 55, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, IoBusMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
