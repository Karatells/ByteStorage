package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.menu.CraftingTerminalMenu;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import com.simpleae.bytestorage.network.packet.TerminalActionPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Клієнтський екран Термінала Крафту SimpleAE (Crafting Terminal GUI).
 *
 * Відображає крафтову сітку 3x3 зліва, результат крафту,
 * віртуальний пул предметів ME-сховища справа, та інвентар гравця знизу.
 */
public class CraftingTerminalScreen extends AbstractContainerScreen<CraftingTerminalMenu> {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private EditBox searchBox;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS = 3;

    // Зміщення віртуального пулу (ME-сховища) у GUI
    private static final int POOL_X_OFFSET = 7;
    private static final int POOL_Y_OFFSET = 76;

    public CraftingTerminalScreen(CraftingTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.inventoryLabelY = 134;
    }

    @Override
    protected void init() {
        super.init();
        // Поле пошуку у правий верхній кут, щоб не накладатися на крафтову сітку
        this.searchBox = new EditBox(this.font, this.leftPos + 88, this.topPos + 6, 88, 12, Component.literal("Пошук"));
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.addWidget(this.searchBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.renderVirtualPoolTooltips(graphics, mouseX, mouseY);
        this.renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Основне вікно у ванильному стилі з фасками (уніфікований рендер рамки)
        GuiStyle.renderWindowFrame(graphics, x, y, imageWidth, imageHeight);

        // ===== КРАФТОВА СЕКЦІЯ (верхня частина) =====

        // Рамка крафтової сітки 3x3
        int craftGridX = x + 30;
        int craftGridY = y + 17;
        graphics.fill(craftGridX - 2, craftGridY - 2, craftGridX + 54 + 2, craftGridY + 54 + 2, GuiStyle.COLOR_PANEL_BG);

        // Слоти крафтової сітки 3x3 (помаранчево-коричневий відтінок для відрізнення)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotBoxX = craftGridX + col * 18;
                int slotBoxY = craftGridY + row * 18;
                GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, GuiStyle.COLOR_SLOT_CRAFT);
            }
        }

        // Стрілка крафту →
        int arrowX = craftGridX + 62;
        int arrowY = craftGridY + 18;
        graphics.fill(arrowX, arrowY + 4, arrowX + 16, arrowY + 12, 0xFF555555);
        graphics.fill(arrowX + 1, arrowY + 5, arrowX + 15, arrowY + 11, 0xFFA0A0A0);
        // Вістря стрілки
        graphics.fill(arrowX + 12, arrowY + 2, arrowX + 16, arrowY + 14, 0xFF555555);
        graphics.fill(arrowX + 13, arrowY + 3, arrowX + 15, arrowY + 13, 0xFFA0A0A0);

        // Слот результату (золотий відтінок)
        int resultSlotX = x + 124;
        int resultSlotY = y + 35;
        graphics.fill(resultSlotX - 3, resultSlotY - 3, resultSlotX + 21, resultSlotY + 21, GuiStyle.COLOR_PANEL_BG);
        GuiStyle.renderSlotBox(graphics, resultSlotX, resultSlotY, GuiStyle.COLOR_SLOT_RESULT);

        // ===== СЕКЦІЯ ME-СХОВИЩА (середня частина) =====

        // Кнопка швидкого вкладання інвентаря [ ↓ ]
        int btnX = x + 172;
        int btnY = y + 61;
        boolean btnHover = mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 13;
        graphics.fill(btnX, btnY, btnX + 14, btnY + 13, GuiStyle.COLOR_PANEL_BG);
        graphics.fill(btnX + 1, btnY + 1, btnX + 13, btnY + 12, btnHover ? GuiStyle.COLOR_BUTTON_HOVER : GuiStyle.COLOR_BUTTON_IDLE);
        graphics.drawString(this.font, "↓", btnX + 4, btnY + 2, 0x1A1A1A, false);

        // Область віртуального сховища (9 колонок x 3 рядки)
        int poolX = x + POOL_X_OFFSET;
        int poolY = y + POOL_Y_OFFSET;
        graphics.fill(poolX - 1, poolY - 1, poolX + 18 * ITEMS_PER_ROW + 1, poolY + 18 * VISIBLE_ROWS + 1, GuiStyle.COLOR_PANEL_BG);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int slotBoxX = poolX + col * 18;
                int slotBoxY = poolY + row * 18;
                GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, GuiStyle.COLOR_SLOT_OUTPUT);
            }
        }

        // Смуга прокрутки справа від слотів ME
        int scrollX = x + 172;
        int scrollY = poolY;
        int scrollH = 18 * VISIBLE_ROWS;
        graphics.fill(scrollX, scrollY, scrollX + 14, scrollY + scrollH, GuiStyle.COLOR_SCROLL_TRACK);
        graphics.fill(scrollX + 1, scrollY + 1, scrollX + 13, scrollY + scrollH - 1, GuiStyle.COLOR_SCROLL_THUMB);
        renderScrollBar(graphics, scrollX + 1, scrollY + 1, 12, scrollH - 2);

        // ===== СЕКЦІЯ ІНВЕНТАРЯ ГРАВЦЯ (нижня частина) =====

        // 3 рядки по 9 слотів
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotBoxX = x + 8 + col * 18;
                int slotBoxY = y + 148 + row * 18;
                GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, 0xFF8B8B8B);
            }
        }

        // Хотбар
        for (int col = 0; col < 9; col++) {
            int slotBoxX = x + 8 + col * 18;
            int slotBoxY = y + 202;
            GuiStyle.renderSlotBox(graphics, slotBoxX, slotBoxY, 0xFF8B8B8B);
        }

        // Отрисовка предметів віртуального пулу
        renderVirtualPoolItems(graphics, mouseX, mouseY);
    }

    private void renderScrollBar(GuiGraphics graphics, int x, int y, int width, int height) {
        List<ItemStack> filtered = getFilteredItems();
        int maxRows = (int) Math.ceil((double) filtered.size() / ITEMS_PER_ROW);
        int maxScroll = Math.max(0, maxRows - VISIBLE_ROWS);

        int thumbH = Math.max(14, height / Math.max(1, maxScroll + 1));
        int thumbY = y;
        if (maxScroll > 0) {
            thumbY += (int) ((height - thumbH) * ((double) scrollOffset / maxScroll));
        }

        // Повзунок
        graphics.fill(x, thumbY, x + width, thumbY + thumbH, GuiStyle.COLOR_WINDOW_BG);
        graphics.fill(x, thumbY, x + width, thumbY + 1, GuiStyle.COLOR_WINDOW_BORDER_LIGHT);
        graphics.fill(x, thumbY, x + 1, thumbY + thumbH, GuiStyle.COLOR_WINDOW_BORDER_LIGHT);
        graphics.fill(x, thumbY + thumbH - 1, x + width, thumbY + thumbH, GuiStyle.COLOR_WINDOW_BORDER_DARK);
        graphics.fill(x + width - 1, thumbY, x + width, thumbY + thumbH, GuiStyle.COLOR_WINDOW_BORDER_DARK);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);

        int used = menu.getClientTotalUsed();
        int max = menu.getClientTotalCapacity();
        double pct = max > 0 ? ((double) used / max) * 100.0 : 0.0;

        String byteText = FORMAT.format(used) + " / " + FORMAT.format(max) + " Б (" + String.format(Locale.ROOT, "%.0f", pct) + "%)";
        int textColor = used >= max && max > 0 ? 0xAA0000 : 0x006600;

        // Заголовок інвентаря і статистика байтів в одному рядку під пулом
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
        int statsWidth = this.font.width(byteText);
        graphics.drawString(this.font, byteText, this.imageWidth - 8 - statsWidth, this.inventoryLabelY, textColor, false);
    }

    private void renderVirtualPoolItems(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ItemStack> filtered = getFilteredItems();
        int start = scrollOffset * ITEMS_PER_ROW;

        for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
            int itemIndex = start + i;
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;

            int itemX = this.leftPos + POOL_X_OFFSET + 1 + col * 18;
            int itemY = this.topPos + POOL_Y_OFFSET + 1 + row * 18;

            // Підсвічування слота при наведенні курсора
            if (mouseX >= itemX - 1 && mouseX < itemX + 17 && mouseY >= itemY - 1 && mouseY < itemY + 17) {
                graphics.fill(itemX - 1, itemY - 1, itemX + 17, itemY + 17, 0x50FFFFFF);
            }

            if (itemIndex < filtered.size()) {
                ItemStack stack = filtered.get(itemIndex);
                graphics.renderItem(stack, itemX, itemY);
                String countText = formatCount(stack.getCount());
                graphics.renderItemDecorations(this.font, stack, itemX, itemY, countText);
            }
        }
    }

    private void renderVirtualPoolTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ItemStack> filtered = getFilteredItems();
        int start = scrollOffset * ITEMS_PER_ROW;

        for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
            int itemIndex = start + i;
            if (itemIndex >= filtered.size()) break;

            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            int itemX = this.leftPos + POOL_X_OFFSET + 1 + col * 18;
            int itemY = this.topPos + POOL_Y_OFFSET + 1 + row * 18;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                ItemStack stack = filtered.get(itemIndex);
                List<Component> tooltip = getTooltipFromContainerItem(stack);
                tooltip.add(Component.literal("Збережено: " + FORMAT.format(stack.getCount()) + " шт.")
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("[ЛКМ] - взяти стак | [ПКМ] - половину | [Shift+ЛКМ] - в інвентар")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                break;
            }
        }
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnX = this.leftPos + 172;
        int btnY = this.topPos + 61;
        if (mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 13) {
            List<Component> tips = new ArrayList<>();
            tips.add(Component.literal("Швидко вкласти інвентар").withStyle(ChatFormatting.YELLOW));
            tips.add(Component.literal("Shift+Клік: включаючи хотбар").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, tips, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Клік по кнопці швидкої вигрузки інвентаря [ ↓ ]
        int btnX = this.leftPos + 172;
        int btnY = this.topPos + 61;
        if (mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 13) {
            int flag = hasShiftDown() ? 1 : 0;
            SimpleByteStoragePackets.sendToServer(
                    new TerminalActionPacket(new ItemStack(Items.AIR, flag), TerminalActionPacket.ACTION_DEPOSIT_ALL)
            );
            return true;
        }

        // Клік по області віртуального пулу ME-сховища
        int poolLeft = leftPos + POOL_X_OFFSET;
        int poolTop = topPos + POOL_Y_OFFSET;
        int poolRight = poolLeft + 18 * ITEMS_PER_ROW;
        int poolBottom = poolTop + 18 * VISIBLE_ROWS;

        if (mouseX >= poolLeft && mouseX < poolRight &&
            mouseY >= poolTop && mouseY < poolBottom) {

            // А) Якщо на курсорі є предмет — вставляємо в сховище
            if (!menu.getCarried().isEmpty()) {
                if (button == 0) {
                    SimpleByteStoragePackets.sendToServer(
                            new TerminalActionPacket(menu.getCarried(), TerminalActionPacket.ACTION_INSERT_CURSOR)
                    );
                    return true;
                } else if (button == 1) {
                    SimpleByteStoragePackets.sendToServer(
                            new TerminalActionPacket(menu.getCarried(), TerminalActionPacket.ACTION_INSERT_ONE)
                    );
                    return true;
                }
            }

            // Б) Якщо курсор порожній — вилучаємо предмет
            List<ItemStack> filtered = getFilteredItems();
            int start = scrollOffset * ITEMS_PER_ROW;

            for (int i = 0; i < ITEMS_PER_ROW * VISIBLE_ROWS; i++) {
                int itemIndex = start + i;
                if (itemIndex >= filtered.size()) break;

                int row = i / ITEMS_PER_ROW;
                int col = i % ITEMS_PER_ROW;
                int itemX = this.leftPos + POOL_X_OFFSET + 1 + col * 18;
                int itemY = this.topPos + POOL_Y_OFFSET + 1 + row * 18;

                if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                    ItemStack clicked = filtered.get(itemIndex);
                    int actionType = TerminalActionPacket.ACTION_EXTRACT_STACK;
                    if (hasShiftDown()) {
                        actionType = TerminalActionPacket.ACTION_EXTRACT_SHIFT;
                    } else if (button == 1) {
                        actionType = TerminalActionPacket.ACTION_EXTRACT_HALF;
                    }
                    SimpleByteStoragePackets.sendToServer(new TerminalActionPacket(clicked, actionType));
                    return true;
                }
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<ItemStack> filtered = getFilteredItems();
        int maxRows = (int) Math.ceil((double) filtered.size() / ITEMS_PER_ROW);
        int maxScroll = Math.max(0, maxRows - VISIBLE_ROWS);

        if (delta < 0) {
            scrollOffset = Math.min(scrollOffset + 1, maxScroll);
        } else if (delta > 0) {
            scrollOffset = Math.max(scrollOffset - 1, 0);
        }
        return true;
    }

    private List<ItemStack> getFilteredItems() {
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        List<ItemStack> pool = menu.getClientVirtualPool();
        if (query.isEmpty()) {
            return pool;
        }
        return pool.stream()
                .filter(s -> s.getHoverName().getString().toLowerCase().contains(query))
                .toList();
    }

    private String formatCount(int count) {
        if (count >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}
