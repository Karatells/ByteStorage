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
    private UiButton filterModeButton;
    private UiButton redstoneButton;

    public IoBusScreen(IoBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.inventoryLabelY = 103;
    }

    @Override
    protected void init() {
        super.init();
        this.modeButton = new UiButton(this.leftPos + 8, this.topPos + 51, 88, 16,
                () -> Component.translatable(this.menu.isExportMode()
                        ? "gui.simple_bytestorage.iobus.export" : "gui.simple_bytestorage.iobus.import"),
                UiButton.Icon.NONE, () -> clickBtn(IoBusMenu.BUTTON_MODE)).primary().icon("arrows");

        this.filterModeButton = new UiButton(this.leftPos + 81, this.topPos + 78, 31, 16,
                () -> Component.translatable(this.menu.isBlacklistMode()
                        ? "gui.simple_bytestorage.iobus.blacklist" : "gui.simple_bytestorage.iobus.whitelist"),
                UiButton.Icon.NONE, () -> clickBtn(IoBusMenu.BUTTON_BLACKLIST))
                .tooltip(Component.translatable("gui.simple_bytestorage.iobus.filter_mode.tip"));

        this.redstoneButton = new UiButton(this.leftPos + 114, this.topPos + 78, 56, 16,
                () -> Component.translatable("gui.simple_bytestorage.iobus.redstone." + this.menu.getRedstoneMode()),
                UiButton.Icon.NONE, () -> clickBtn(IoBusMenu.BUTTON_REDSTONE))
                .tooltip(Component.translatable("gui.simple_bytestorage.iobus.redstone.tip"));
    }

    private void clickBtn(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.modeButton.mouseClicked(mouseX, mouseY, button)
                || this.filterModeButton.mouseClicked(mouseX, mouseY, button)
                || this.redstoneButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.modeButton.render(graphics, this.font, mouseX, mouseY);
        this.filterModeButton.render(graphics, this.font, mouseX, mouseY);
        this.redstoneButton.render(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.modeButton.isOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font, Component.translatable(this.menu.isExportMode()
                    ? "gui.simple_bytestorage.iobus.export.tip" : "gui.simple_bytestorage.iobus.import.tip"),
                    mouseX, mouseY);
        }
        this.filterModeButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.redstoneButton.renderTooltip(graphics, this.font, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.getMachines() > 0 ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "cable");

        GuiStyle.panel(graphics, x + 6, y + 20, 164, 24);
        for (int i = 0; i < IoBusMenu.FILTER_SLOTS; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.panel(graphics, x + 6, y + 74, 74, 24);
        for (int i = 0; i < IoBusMenu.UPGRADE_SLOTS; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(IoBusMenu.UPGRADE_START + i), GuiStyle.SLOT_CARD);
        }

        GuiStyle.display(graphics, x + 6, y + 48, 164, 22);
        Component status = Component.translatable(!menu.isOnline()
                ? "gui.simple_bytestorage.provider.offline"
                : (menu.getMachines() > 0 ? "gui.simple_bytestorage.storage_bus.ok"
                : "gui.simple_bytestorage.storage_bus.none"));
        int color = !menu.isOnline() ? GuiStyle.TEXT_BAD : (menu.getMachines() > 0 ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN);
        graphics.drawString(this.font, GuiStyle.clip(this.font, status.getString(), 68), x + 100, y + 55, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, IoBusMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
