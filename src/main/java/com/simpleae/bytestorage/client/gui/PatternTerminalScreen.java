package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import com.simpleae.bytestorage.network.packet.PatternOrderPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Екран глобального Терміналу Шаблонів.
 *
 *   [ Пошук рецептів...        ] [ Усі ]
 *   [іконка] Назва / статус      [Замовити] [×]
 *   ...
 *   Статус мережі: пам'ять X / Y байт · Асемблерів: N
 */
public class PatternTerminalScreen extends AbstractContainerScreen<PatternTerminalMenu> {

    private static final int ROWS = 6;
    private static final int ROW_H = 24;
    private static final int LIST_X = 10;   // відносно leftPos
    private static final int LIST_Y = 38;   // відносно topPos
    private static final int ROW_W = 198;
    private static final int ORDER_W = 50;
    private static final int REMOVE_W = 14;
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private EditBox searchBox;
    private Button filterButton;
    private boolean onlyCraftable = false;
    private int scrollOffset = 0;

    public PatternTerminalScreen(PatternTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 212;
    }

    @Override
    protected void init() {
        super.init();

        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 17, 148, 16,
                Component.translatable("gui.simple_bytestorage.pattern.search"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.pattern.search"));
        this.searchBox.setResponder(value -> this.scrollOffset = 0);
        this.addRenderableWidget(this.searchBox);

        this.filterButton = Button.builder(filterLabel(), button -> {
            this.onlyCraftable = !this.onlyCraftable;
            this.scrollOffset = 0;
            button.setMessage(filterLabel());
        }).bounds(this.leftPos + 160, this.topPos + 17, 62, 16).build();
        this.addRenderableWidget(this.filterButton);
    }

    private Component filterLabel() {
        return Component.translatable(this.onlyCraftable
                ? "gui.simple_bytestorage.pattern.filter.ready"
                : "gui.simple_bytestorage.pattern.filter.all");
    }

    private List<PatternData> filtered() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<PatternData> result = new ArrayList<>();
        for (PatternData pattern : this.menu.getPatterns()) {
            if (this.onlyCraftable && !this.menu.isCraftable(pattern.getId())) {
                continue;
            }
            if (query.isEmpty()
                    || pattern.getOutput().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(pattern);
            }
        }
        return result;
    }

    private int maxScroll(int size) {
        return Math.max(0, size - ROWS);
    }

    // --- Відмальовування ---

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderList(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiStyle.renderWindowFrame(graphics, x, y, this.imageWidth, this.imageHeight);
        GuiStyle.renderPanel(graphics, x + 8, y + 36, 214, 148, null);
        GuiStyle.renderPanel(graphics, x + 8, y + 188, 214, 18, null);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, GuiStyle.COLOR_TEXT_TITLE, false);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        List<PatternData> list = filtered();
        int max = maxScroll(list.size());
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, max));

        List<Component> hoverTooltip = null;
        int end = Math.min(list.size(), this.scrollOffset + ROWS);

        for (int i = this.scrollOffset; i < end; i++) {
            PatternData pattern = list.get(i);
            int rowX = x + LIST_X;
            int rowY = y + LIST_Y + (i - this.scrollOffset) * ROW_H;
            boolean ready = this.menu.isCraftable(pattern.getId());
            boolean rowHover = inside(mouseX, mouseY, rowX, rowY, ROW_W, ROW_H - 2);

            graphics.fill(rowX, rowY, rowX + ROW_W, rowY + ROW_H - 2, rowHover ? 0xFF3A404A : 0xFF2B3038);
            graphics.fill(rowX, rowY, rowX + 2, rowY + ROW_H - 2, ready ? 0xFF00B464 : 0xFF8A3A2A);

            ItemStack output = pattern.getOutput();
            graphics.renderItem(output, rowX + 5, rowY + 3);
            graphics.renderItemDecorations(this.font, output, rowX + 5, rowY + 3);

            int textMax = ROW_W - 26 - ORDER_W - REMOVE_W - 10;
            graphics.drawString(this.font,
                    this.font.plainSubstrByWidth(output.getHoverName().getString(), textMax),
                    rowX + 26, rowY + 3, 0xFFFFFFFF, false);
            Component status = Component.translatable(ready
                    ? "gui.simple_bytestorage.pattern.ready"
                    : "gui.simple_bytestorage.pattern.missing");
            graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), textMax),
                    rowX + 26, rowY + 13, ready ? 0xFF55FF55 : 0xFFFF7755, false);

            int removeX = rowX + ROW_W - REMOVE_W - 3;
            int orderX = removeX - ORDER_W - 2;
            int btnY = rowY + 2;
            boolean orderHover = inside(mouseX, mouseY, orderX, btnY, ORDER_W, 18);
            boolean removeHover = inside(mouseX, mouseY, removeX, btnY, REMOVE_W, 18);

            drawButton(graphics, orderX, btnY, ORDER_W, 18, orderHover,
                    Component.translatable("gui.simple_bytestorage.pattern.order").getString());
            drawButton(graphics, removeX, btnY, REMOVE_W, 18, removeHover, "×");

            if (orderHover) {
                hoverTooltip = List.of(
                        Component.translatable("gui.simple_bytestorage.pattern.order.tip1"),
                        Component.translatable("gui.simple_bytestorage.pattern.order.tip2"));
            } else if (removeHover) {
                hoverTooltip = List.of(Component.translatable("gui.simple_bytestorage.pattern.remove"));
            } else if (rowHover) {
                hoverTooltip = patternTooltip(pattern);
            }
        }

        if (list.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable(this.menu.getPatterns().isEmpty()
                            ? "gui.simple_bytestorage.pattern.empty"
                            : "gui.simple_bytestorage.pattern.nothing_found").getString(),
                    x + this.imageWidth / 2, y + 100, GuiStyle.COLOR_TEXT_MUTED);
        }

        if (list.size() > ROWS) {
            GuiStyle.renderScrollBar(graphics, x + 210, y + LIST_Y, 8, ROWS * ROW_H - 2,
                    this.scrollOffset, max, ROWS, list.size());
        }

        renderStatus(graphics);

        if (hoverTooltip != null) {
            graphics.renderComponentTooltip(this.font, hoverTooltip, mouseX, mouseY);
        }
    }

    private List<Component> patternTooltip(PatternData pattern) {
        List<Component> lines = new ArrayList<>();
        lines.add(pattern.getOutput().getHoverName().copy()
                .append(Component.literal(" x" + pattern.getOutput().getCount())));
        lines.add(Component.translatable("tooltip.simple_bytestorage.pattern.ingredients"));
        for (ItemStack input : pattern.getInputs()) {
            lines.add(Component.literal("  • ").append(input.getHoverName()).append(" x" + input.getCount()));
        }
        return lines;
    }

    private void drawButton(GuiGraphics graphics, int bx, int by, int bw, int bh, boolean hover, String text) {
        graphics.fill(bx, by, bx + bw, by + bh, hover ? GuiStyle.COLOR_BUTTON_HOVER : GuiStyle.COLOR_BUTTON_IDLE);
        graphics.fill(bx, by, bx + bw, by + 1, 0xFF7F8998);
        graphics.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF20242A);
        graphics.drawCenteredString(this.font, text, bx + bw / 2, by + (bh - 8) / 2, GuiStyle.COLOR_BUTTON_TEXT);
    }

    private void renderStatus(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        String text;
        int color;
        if (this.menu.getCapacityBytes() <= 0) {
            text = Component.translatable("gui.simple_bytestorage.pattern.no_network").getString();
            color = GuiStyle.COLOR_PANEL_HEADER_TEXT;
        } else {
            text = Component.translatable("gui.simple_bytestorage.pattern.status",
                    FORMAT.format(this.menu.getUsedBytes()), FORMAT.format(this.menu.getCapacityBytes()),
                    this.menu.getAssemblerCount()).getString();
            color = 0xFF55FF55;
        }
        graphics.drawString(this.font, this.font.plainSubstrByWidth(text, 206), x + 12, y + 193, color, false);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --- Ввід ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }

        List<PatternData> list = filtered();
        int end = Math.min(list.size(), this.scrollOffset + ROWS);
        for (int i = this.scrollOffset; i < end; i++) {
            int rowX = this.leftPos + LIST_X;
            int rowY = this.topPos + LIST_Y + (i - this.scrollOffset) * ROW_H;
            int removeX = rowX + ROW_W - REMOVE_W - 3;
            int orderX = removeX - ORDER_W - 2;
            PatternData pattern = list.get(i);

            if (inside(mouseX, mouseY, orderX, rowY + 2, ORDER_W, 18)) {
                int amount = Screen.hasControlDown() ? 64 : (Screen.hasShiftDown() ? 10 : 1);
                SimpleByteStoragePackets.sendToServer(
                        new PatternOrderPacket(PatternOrderPacket.ACTION_ORDER, pattern.getId(), amount));
                return true;
            }
            if (inside(mouseX, mouseY, removeX, rowY + 2, REMOVE_W, 18)) {
                if (Screen.hasShiftDown()) {
                    SimpleByteStoragePackets.sendToServer(
                            new PatternOrderPacket(PatternOrderPacket.ACTION_REMOVE, pattern.getId(), 0));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = maxScroll(filtered().size());
        if (delta < 0) {
            this.scrollOffset = Math.min(this.scrollOffset + 1, max);
        } else if (delta > 0) {
            this.scrollOffset = Math.max(this.scrollOffset - 1, 0);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Поки введення активне, клавіші (зокрема «E») не повинні закривати вікно.
        if (keyCode != 256 && this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
