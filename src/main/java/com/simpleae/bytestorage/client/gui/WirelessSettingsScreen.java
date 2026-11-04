package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.item.WirelessConfig;
import com.simpleae.bytestorage.menu.WirelessSettingsMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Экран настроек Беспроводного терминала. */
public class WirelessSettingsScreen extends AbstractContainerScreen<WirelessSettingsMenu> {

    private final List<UiButton> buttons = new ArrayList<>();

    public WirelessSettingsScreen(WirelessSettingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
    }

    private void click(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();
        buttons.add(new UiButton(leftPos + 8, topPos + 44, 52, 16,
                () -> Component.translatable(menu.isRestock() ? "gui.simple_bytestorage.wireless.restock.on" : "gui.simple_bytestorage.wireless.restock.off"),
                UiButton.Icon.NONE, () -> click(0)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.restock.tip")));
        buttons.add(new UiButton(leftPos + 62, topPos + 44, 52, 16,
                () -> Component.translatable(menu.isPickup() ? "gui.simple_bytestorage.wireless.pickup.on" : "gui.simple_bytestorage.wireless.pickup.off"),
                UiButton.Icon.NONE, () -> click(1)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.pickup.tip")));
        buttons.add(new UiButton(leftPos + 116, topPos + 44, 52, 16,
                () -> Component.translatable(menu.isWhitelist() ? "gui.simple_bytestorage.wireless.whitelist" : "gui.simple_bytestorage.wireless.blacklist"),
                UiButton.Icon.NONE, () -> click(2)).tooltip(Component.translatable("gui.simple_bytestorage.wireless.list.tip")));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (UiButton b : buttons) {
            if (b.mouseClicked(mx, my, button)) return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        for (UiButton b : buttons) {
            b.render(g, this.font, mouseX, mouseY);
        }
        this.renderTooltip(g, mouseX, mouseY);
        for (UiButton b : buttons) {
            if (b.isOver(mouseX, mouseY)) {
                b.renderTooltip(g, this.font, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        GuiStyle.window(g, x, y, imageWidth, imageHeight, this.title, GuiStyle.LED_GOOD);
        GuiStyle.panel(g, x + 6, y + 20, 60, 22);
        for (int i = 0; i < WirelessConfig.UPGRADE_SLOTS; i++) {
            GuiStyle.menuSlot(g, x, y, menu.slots.get(i), GuiStyle.SLOT_CARD, GuiStyle.ACCENT);
        }
        g.drawString(this.font, Component.translatable("gui.simple_bytestorage.wireless.upgrades"), x + 48, y + 28, GuiStyle.TEXT_LABEL, false);
        GuiStyle.label(g, Component.translatable("gui.simple_bytestorage.wireless.filter"), x + 8, y + 64);
        GuiStyle.panel(g, x + 6, y + 73, 164, 24);
        for (int i = 0; i < WirelessConfig.FILTER_SLOTS; i++) {
            GuiStyle.menuSlot(g, x, y, menu.slots.get(WirelessConfig.UPGRADE_SLOTS + i), GuiStyle.SLOT_POOL);
        }
        GuiStyle.inventoryLabel(g, this.playerInventoryTitle, x + 8, y + WirelessSettingsMenu.PLAYER_INV_Y - 10);
        GuiStyle.playerSlots(g, x, y, menu.slots, WirelessSettingsMenu.PLAYER_START);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }
}
