package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.api.storage.IPatternStorage.PatternData;
import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import com.simpleae.bytestorage.network.packet.CraftPlanPacket;
import com.simpleae.bytestorage.network.packet.PatternOrderPacket;
import com.simpleae.bytestorage.network.packet.SimpleByteStoragePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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
    private UiButton filterButton;
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

        this.searchBox = new EditBox(this.font, this.leftPos + 12, this.topPos + 21, 140, 10, Component.empty());
        this.searchBox.setMaxLength(48);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(GuiStyle.TEXT_TITLE);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.pattern.search")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(value -> this.scrollOffset = 0);
        this.addRenderableWidget(this.searchBox);

        this.filterButton = new UiButton(this.leftPos + 160, this.topPos + 19, 62, 14,
                this::filterLabel, UiButton.Icon.NONE, () -> {
            this.onlyCraftable = !this.onlyCraftable;
            this.scrollOffset = 0;
        });
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
        this.filterButton.render(graphics, this.font, mouseX, mouseY);
        this.renderList(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
        if (this.menu.hasClientPlan()) {
            this.renderPlan(graphics, mouseX, mouseY);
        }
    }

    // --- Вікно підтвердження замовлення ---

    private static final int PLAN_X = 14;
    private static final int PLAN_Y = 40;
    private static final int PLAN_W = 202;
    private static final int PLAN_H = 140;

    private boolean planOk() {
        return this.menu.getPlanMissing().isEmpty() && !this.menu.getPlanSteps().isEmpty();
    }

    private void renderPlan(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos + PLAN_X;
        int y = this.topPos + PLAN_Y;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        graphics.fill(this.leftPos + 6, this.topPos + 18, this.leftPos + this.imageWidth - 6,
                this.topPos + this.imageHeight - 6, 0xB0000000);
        GuiStyle.panel(graphics, x, y, PLAN_W, PLAN_H);
        graphics.drawString(this.font, Component.translatable("gui.simple_bytestorage.plan.title",
                this.menu.getPlanAmount()), x + 6, y + 5, GuiStyle.TEXT_TITLE, false);

        GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.plan.steps"), x + 6, y + 17);
        int line = y + 28;
        List<CraftPlanPacket.Entry> steps = this.menu.getPlanSteps();
        for (int i = 0; i < steps.size() && i < 6; i++) {
            CraftPlanPacket.Entry entry = steps.get(i);
            String text = entry.count + "× " + entry.stack.getHoverName().getString();
            if (entry.processing) {
                text += " " + Component.translatable("gui.simple_bytestorage.pattern.tag.process").getString();
            }
            graphics.drawString(this.font, this.font.plainSubstrByWidth(text, PLAN_W - 16), x + 10, line,
                    GuiStyle.TEXT_GOOD, false);
            line += 10;
        }
        if (steps.size() > 6) {
            graphics.drawString(this.font, "+" + (steps.size() - 6), x + 10, line, GuiStyle.TEXT_MUTED, false);
        }

        List<CraftPlanPacket.Entry> missing = this.menu.getPlanMissing();
        if (!missing.isEmpty()) {
            GuiStyle.label(graphics, Component.translatable("gui.simple_bytestorage.plan.missing"), x + 6, y + 92);
            int mline = y + 102;
            for (int i = 0; i < missing.size() && i < 3; i++) {
                CraftPlanPacket.Entry entry = missing.get(i);
                String text = entry.count + "× " + entry.stack.getHoverName().getString();
                graphics.drawString(this.font, this.font.plainSubstrByWidth(text, PLAN_W - 16), x + 10, mline,
                        GuiStyle.TEXT_BAD, false);
                mline += 10;
            }
        }

        UiButton.paint(graphics, this.font, x + 6, y + PLAN_H - 20, 92, 14,
                Component.translatable("gui.simple_bytestorage.plan.start").getString(),
                inside(mouseX, mouseY, x + 6, y + PLAN_H - 20, 92, 14), planOk());
        UiButton.paint(graphics, this.font, x + PLAN_W - 98, y + PLAN_H - 20, 92, 14,
                Component.translatable("gui.simple_bytestorage.plan.cancel").getString(),
                inside(mouseX, mouseY, x + PLAN_W - 98, y + PLAN_H - 20, 92, 14), true);
        graphics.pose().popPose();
    }

    private boolean handlePlanClick(double mouseX, double mouseY, int button) {
        int x = this.leftPos + PLAN_X;
        int y = this.topPos + PLAN_Y;
        if (button == 0 && inside(mouseX, mouseY, x + 6, y + PLAN_H - 20, 92, 14) && planOk()) {
            SimpleByteStoragePackets.sendToServer(new PatternOrderPacket(PatternOrderPacket.ACTION_ORDER,
                    this.menu.getPlanPatternId(), this.menu.getPlanAmount()));
            this.menu.clearClientPlan();
        } else if (button == 0 && inside(mouseX, mouseY, x + PLAN_W - 98, y + PLAN_H - 20, 92, 14)) {
            this.menu.clearClientPlan();
        }
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int led = this.menu.getCapacityBytes() <= 0 ? GuiStyle.LED_BAD : GuiStyle.LED_GOOD;
        GuiStyle.window(graphics, x, y, this.imageWidth, this.imageHeight, this.title, led);
        GuiStyle.display(graphics, x + 8, y + 19, 148, 14);
        GuiStyle.display(graphics, x + 8, y + 36, 214, 148);
        GuiStyle.panel(graphics, x + 8, y + 188, 214, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Усе малюється у renderBg через GuiStyle
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

            graphics.fill(rowX, rowY, rowX + ROW_W, rowY + ROW_H - 2, rowHover ? 0xFF1E2E40 : 0xFF141B26);
            graphics.fill(rowX, rowY, rowX + 2, rowY + ROW_H - 2, ready ? GuiStyle.LED_GOOD : GuiStyle.LED_BAD);

            ItemStack output = pattern.getOutput();
            graphics.renderItem(output, rowX + 5, rowY + 3);
            graphics.renderItemDecorations(this.font, output, rowX + 5, rowY + 3);

            int textMax = ROW_W - 26 - ORDER_W - REMOVE_W - 10;
            graphics.drawString(this.font,
                    this.font.plainSubstrByWidth(output.getHoverName().getString(), textMax),
                    rowX + 26, rowY + 3, GuiStyle.TEXT_TITLE, false);
            Component status = Component.translatable(ready
                    ? "gui.simple_bytestorage.pattern.ready"
                    : "gui.simple_bytestorage.pattern.missing");
            int statusX = rowX + 26;
            if (pattern.isProcessing()) {
                String tag = Component.translatable("gui.simple_bytestorage.pattern.tag.process").getString();
                graphics.drawString(this.font, tag, statusX, rowY + 13, GuiStyle.ACCENT, false);
                statusX += this.font.width(tag) + 4;
            }
            graphics.drawString(this.font, this.font.plainSubstrByWidth(status.getString(), textMax - (statusX - rowX - 26)),
                    statusX, rowY + 13, ready ? GuiStyle.TEXT_GOOD : GuiStyle.TEXT_WARN, false);

            int removeX = rowX + ROW_W - REMOVE_W - 3;
            int orderX = removeX - ORDER_W - 2;
            int btnY = rowY + 2;
            boolean orderHover = inside(mouseX, mouseY, orderX, btnY, ORDER_W, 18);
            boolean removeHover = inside(mouseX, mouseY, removeX, btnY, REMOVE_W, 18);

            UiButton.paint(graphics, this.font, orderX, btnY, ORDER_W, 18,
                    Component.translatable("gui.simple_bytestorage.pattern.order").getString(), orderHover, true);
            UiButton.paint(graphics, this.font, removeX, btnY, REMOVE_W, 18, "×", removeHover, true);

            if (orderHover) {
                hoverTooltip = List.of(
                        Component.translatable("gui.simple_bytestorage.pattern.order.tip1"),
                        Component.translatable("gui.simple_bytestorage.pattern.order.tip2"));
            } else if (removeHover) {
                hoverTooltip = List.of(Component.translatable(pattern.isProcessing()
                        ? "gui.simple_bytestorage.pattern.remove.bus"
                        : "gui.simple_bytestorage.pattern.remove"));
            } else if (rowHover) {
                hoverTooltip = patternTooltip(pattern);
            }
        }

        if (list.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable(this.menu.getPatterns().isEmpty()
                            ? "gui.simple_bytestorage.pattern.empty"
                            : "gui.simple_bytestorage.pattern.nothing_found").getString(),
                    x + this.imageWidth / 2, y + 100, GuiStyle.TEXT_MUTED);
        }

        if (list.size() > ROWS) {
            GuiStyle.scrollbar(graphics, x + 210, y + LIST_Y, 8, ROWS * ROW_H - 2,
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

    private void renderStatus(GuiGraphics graphics) {
        int x = this.leftPos;
        int y = this.topPos;
        String text;
        int color;
        if (this.menu.getCapacityBytes() <= 0) {
            text = Component.translatable("gui.simple_bytestorage.pattern.no_network").getString();
            color = GuiStyle.TEXT_MUTED;
        } else {
            text = Component.translatable("gui.simple_bytestorage.pattern.status",
                    FORMAT.format(this.menu.getUsedBytes()), FORMAT.format(this.menu.getCapacityBytes()),
                    this.menu.getAssemblerCount()).getString();
            color = GuiStyle.TEXT_GOOD;
        }
        graphics.drawString(this.font, this.font.plainSubstrByWidth(text, 206), x + 12, y + 193, color, false);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --- Ввід ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.hasClientPlan()) {
            return handlePlanClick(mouseX, mouseY, button);
        }
        // ВАЖЛИВО: AbstractContainerScreen.mouseClicked завжди повертає true (навіть без слотів),
        // тому кліки по рядках списку обробляємо ДО виклику super.
        this.searchBox.setFocused(this.searchBox.isMouseOver(mouseX, mouseY));
        if (this.searchBox.isFocused()) {
            this.searchBox.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (this.filterButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && handleListClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleListClick(double mouseX, double mouseY) {
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
                        new PatternOrderPacket(PatternOrderPacket.ACTION_PLAN, pattern.getId(), amount));
                return true;
            }
            if (inside(mouseX, mouseY, removeX, rowY + 2, REMOVE_W, 18)) {
                if (Screen.hasShiftDown() && !pattern.isProcessing()) {
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
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.menu.hasClientPlan()) {
            return true;
        }
        if (this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.menu.hasClientPlan()) {
            if (keyCode == 256) {
                this.menu.clearClientPlan();
            }
            return true;
        }
        // Поки введення активне, клавіші (зокрема «E») не повинні закривати вікно.
        if (keyCode != 256 && this.searchBox != null && this.searchBox.isFocused()) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
