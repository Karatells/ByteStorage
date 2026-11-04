package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.api.storage.IByteStorageCell;
import com.simpleae.bytestorage.api.storage.ICellInventory;
import com.simpleae.bytestorage.block.entity.AssemblerBlockEntity;
import com.simpleae.bytestorage.menu.AssemblerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Екран Асемблера.
 *
 *   ┌ Асемблер ───────────────────────────┐
 *   │ [картка] ═══ прогрес ═══►  [результат]
 *   │            статус                    │
 *   │ Буфер (вміст картки): [ ][ ][ ] ...  │
 *   │ [Скасувати]              Черга: N    │
 *   │ Інвентар гравця 3×9 + хотбар         │
 */
public class AssemblerScreen extends AbstractContainerScreen<AssemblerMenu> {

    private static final int BAR_X = 40;
    private static final int BAR_Y = 31;
    private static final int BAR_W = 86;
    private static final int RESULT_X = 145;
    private static final int RESULT_Y = 27;
    private static final int BUFFER_ICONS = 9;

    private UiButton cancelButton;

    public AssemblerScreen(AssemblerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 239;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 148;
    }

    @Override
    protected void init() {
        super.init();
        this.cancelButton = new UiButton(this.leftPos + 8, this.topPos + 127, 70, 16,
                () -> Component.translatable("gui.simple_bytestorage.assembler.cancel"), UiButton.Icon.NONE, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, AssemblerMenu.BUTTON_CANCEL);
            }
        }).enabledWhen(() -> this.menu.getQueue() > 0 || this.menu.isBusy());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.cancelButton.render(graphics, this.font, mouseX, mouseY);
        this.renderDynamic(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        GuiStyle.window(graphics, x, y, this.imageWidth, this.imageHeight, this.title, ledColor());

        // Верхня панель: картка → прогрес → результат
        GuiStyle.panel(graphics, x + 6, y + 18, 164, 44);
        GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(AssemblerMenu.SLOT_CARD), GuiStyle.SLOT_CARD);
        GuiStyle.progress(graphics, x + BAR_X, y + BAR_Y, BAR_W, 10, progressFraction());
        GuiStyle.slot(graphics, x + RESULT_X - 1, y + RESULT_Y - 1, GuiStyle.SLOT_RESULT, GuiStyle.TEXT_WARN);

        // Панель буфера
        GuiStyle.display(graphics, x + 6, y + 66, 164, 36);
        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.assembler.buffer"), x + 10, y + 70);
        for (int i = 0; i < BUFFER_ICONS; i++) {
            GuiStyle.slot(graphics, x + 7 + i * 18, y + 82, GuiStyle.SLOT_POOL);
        }

        GuiStyle.panel(graphics, x + 6, y + 104, 164, 22);
        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.assembler.upgrades"), x + 10, y + 111);
        for (int i = AssemblerMenu.UPGRADE_START; i < AssemblerMenu.UPGRADE_END; i++) {
            GuiStyle.menuSlot(graphics, x, y, this.menu.slots.get(i), GuiStyle.SLOT_OUTPUT);
        }

        GuiStyle.inventoryLabel(graphics, this.playerInventoryTitle, x + this.inventoryLabelX, y + this.inventoryLabelY);
        GuiStyle.playerSlots(graphics, x, y, this.menu.slots, AssemblerMenu.PLAYER_START);
    }

    private int ledColor() {
        if (this.menu.getCard().isEmpty()) {
            return GuiStyle.LED_BAD;
        }
        if (this.menu.getPhase() != AssemblerBlockEntity.PHASE_IDLE) {
            return GuiStyle.LED_GOOD;
        }
        if (this.menu.getQueue() > 0 && this.menu.getWaitReason() != AssemblerBlockEntity.WAIT_NONE) {
            return GuiStyle.LED_WARN;
        }
        return 0xFF4A5260;
    }

    private float progressFraction() {
        if (this.menu.getPhase() == AssemblerBlockEntity.PHASE_CRAFTING) {
            return (float) this.menu.getProgress() / this.menu.getProgressMax();
        }
        if (this.menu.getPhase() == AssemblerBlockEntity.PHASE_DELIVERING) {
            return 1.0F;
        }
        return 0.0F;
    }

    /** Предмети поверх фону: прев'ю результату, вміст буфера, статус, черга. */
    private void renderDynamic(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Прев'ю результату поточного крафту
        ItemStack output = this.menu.getCurrentOutput();
        if (!output.isEmpty()
                && this.menu.getPhase() != AssemblerBlockEntity.PHASE_IDLE) {
            graphics.renderItem(output, x + RESULT_X, y + RESULT_Y);
            graphics.renderItemDecorations(this.font, output, x + RESULT_X, y + RESULT_Y);
            if (isOver(mouseX, mouseY, x + RESULT_X, y + RESULT_Y, 16, 16)) {
                graphics.renderTooltip(this.font, output, mouseX, mouseY);
            }
        }

        // Статус
        Component status = statusText();
        graphics.drawCenteredString(this.font, status, x + 88, y + 48, statusColor());

        // Вміст буфера (картки)
        ItemStack card = this.menu.getCard();
        if (!card.isEmpty() && card.getItem() instanceof IByteStorageCell cell) {
            ICellInventory inventory = cell.getCellInventory(card);
            List<ItemStack> stored = inventory.getStoredItems();
            int shown = Math.min(stored.size(), BUFFER_ICONS);
            for (int i = 0; i < shown; i++) {
                ItemStack stack = stored.get(i);
                int ix = x + 8 + i * 18;
                int iy = y + 83;
                graphics.renderItem(stack, ix, iy);
                GuiStyle.itemCount(graphics, this.font, stack, ix, iy, shortCount(stack.getCount()));
                if (isOver(mouseX, mouseY, ix, iy, 16, 16)) {
                    graphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
            if (stored.size() > BUFFER_ICONS) {
                String more = "+" + (stored.size() - BUFFER_ICONS);
                graphics.drawString(this.font, more, x + 166 - this.font.width(more), y + 70,
                        GuiStyle.TEXT_LABEL, false);
            }
        }

        // Черга
        String queue = Component.translatable("gui.simple_bytestorage.assembler.queue", this.menu.getQueue()).getString();
        graphics.drawString(this.font, queue, x + 170 - this.font.width(queue), y + 131,
                GuiStyle.TEXT_TITLE, false);
    }

    private Component statusText() {
        if (this.menu.getCard().isEmpty()) {
            return Component.translatable("gui.simple_bytestorage.assembler.wait.no_card");
        }
        switch (this.menu.getPhase()) {
            case AssemblerBlockEntity.PHASE_CRAFTING:
                return Component.translatable("gui.simple_bytestorage.assembler.crafting");
            case AssemblerBlockEntity.PHASE_DELIVERING:
                return this.menu.getWaitReason() == AssemblerBlockEntity.WAIT_NETWORK_FULL
                        ? Component.translatable("gui.simple_bytestorage.assembler.wait.network_full")
                        : Component.translatable("gui.simple_bytestorage.assembler.delivering");
            default:
                break;
        }
        if (this.menu.getQueue() <= 0) {
            return Component.translatable("gui.simple_bytestorage.assembler.idle");
        }
        switch (this.menu.getWaitReason()) {
            case AssemblerBlockEntity.WAIT_RESOURCES:
                return Component.translatable("gui.simple_bytestorage.assembler.wait.resources");
            case AssemblerBlockEntity.WAIT_CARD_BUSY:
                return Component.translatable("gui.simple_bytestorage.assembler.wait.card_busy");
            case AssemblerBlockEntity.WAIT_CARD_SMALL:
                return Component.translatable("gui.simple_bytestorage.assembler.wait.card_small");
            case AssemblerBlockEntity.WAIT_NETWORK_OFF:
                return Component.translatable("gui.simple_bytestorage.assembler.wait.network_off");
            default:
                return Component.translatable("gui.simple_bytestorage.assembler.wait.start");
        }
    }

    private int statusColor() {
        if (this.menu.getCard().isEmpty()) {
            return GuiStyle.TEXT_LABEL;
        }
        if (this.menu.getPhase() != AssemblerBlockEntity.PHASE_IDLE) {
            return GuiStyle.TEXT_GOOD;
        }
        if (this.menu.getQueue() > 0 && this.menu.getWaitReason() != AssemblerBlockEntity.WAIT_NONE) {
            return GuiStyle.TEXT_WARN;
        }
        return GuiStyle.TEXT_MUTED;
    }

    private static String shortCount(int count) {
        if (count >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1_000.0);
        }
        return count > 1 ? String.valueOf(count) : "";
    }

    private static boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
    }
}
