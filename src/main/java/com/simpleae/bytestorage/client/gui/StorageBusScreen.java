package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.StorageBusMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Екран Шини зберігання: фільтр, пріоритет та режим білого/чорного списку. */
public class StorageBusScreen extends AbstractContainerScreen<StorageBusMenu> {

    private UiButton prioDownButton;
    private UiButton prioUpButton;
    private UiButton filterModeButton;

    public StorageBusScreen(StorageBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 170;
        this.inventoryLabelY = 77;
    }

    @Override
    protected void init() {
        super.init();
        this.prioDownButton = new UiButton(this.leftPos + 8, this.topPos + 51, 16, 16,
                () -> Component.literal("-"), UiButton.Icon.NONE, () -> clickBtn(StorageBusMenu.BUTTON_PRIO_DOWN));
        this.prioUpButton = new UiButton(this.leftPos + 64, this.topPos + 51, 16, 16,
                () -> Component.literal("+"), UiButton.Icon.NONE, () -> clickBtn(StorageBusMenu.BUTTON_PRIO_UP));
        this.filterModeButton = new UiButton(this.leftPos + 86, this.topPos + 51, 46, 16,
                () -> Component.translatable(this.menu.isBlacklistMode()
                        ? "gui.simple_bytestorage.iobus.blacklist" : "gui.simple_bytestorage.iobus.whitelist"),
                UiButton.Icon.NONE, () -> clickBtn(StorageBusMenu.BUTTON_BLACKLIST))
                .tooltip(Component.translatable("gui.simple_bytestorage.storage_bus.filter.tip"));
    }

    private void clickBtn(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.prioDownButton.mouseClicked(mouseX, mouseY, button)
                || this.prioUpButton.mouseClicked(mouseX, mouseY, button)
                || this.filterModeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.prioDownButton.render(graphics, this.font, mouseX, mouseY);
        this.prioUpButton.render(graphics, this.font, mouseX, mouseY);
        this.filterModeButton.render(graphics, this.font, mouseX, mouseY);
        this.filterModeButton.renderTooltip(graphics, this.font, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        int led = !menu.isOnline() ? GuiStyle.LED_BAD : (menu.getContainers() > 0 ? GuiStyle.LED_GOOD : GuiStyle.LED_WARN);
        GuiStyle.window(graphics, x, y, imageWidth, imageHeight, this.title, led, "drive");

        GuiStyle.panel(graphics, x + 6, y + 20, 164, 24);
        for (int i = 0; i < StorageBusMenu.FILTER_SLOTS; i++) {
            GuiStyle.menuSlot(graphics, x, y, menu.slots.get(i), GuiStyle.SLOT_POOL);
        }

        GuiStyle.display(graphics, x + 6, y + 48, 164, 22);
        String prioText = String.valueOf(menu.getPriority());
        graphics.drawString(this.font, prioText, x + 44 - this.font.width(prioText) / 2, y + 55, GuiStyle.TEXT_TITLE, false);

        Component status = Component.translatable(!menu.isOnline()
                ? "gui.simple_bytestorage.provider.offline"
                : (menu.getContainers() > 0 ? "gui.simple_bytestorage.storage_bus.ok"
                : "gui.simple_bytestorage.storage_bus.none"));
        int color = !menu.isOnline() ? GuiStyle.TEXT_BAD : (menu.getContainers() > 0 ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), 34), x + 135, y + 55, color, false);

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + 8, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, menu.slots, StorageBusMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
