package com.simpleae.bytestorage.client.guide;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.client.gui.GuiStyle;
import com.simpleae.bytestorage.client.gui.UiButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Переработанный экран Книги-справочника Simple Byte Storage:
 * - Поиск по главам и содержимому
 * - 3D-иконки блоков и предметов в оглавлении
 * - Вкладки «Справочник» (Guide) и «Крафты» (Crafts) с интерактивной сеткой 3×3, печью и тултипами
 */
public class GuideScreen extends Screen {

    private static final int W = 424;
    private static final int H = 248;
    private static final int LIST_X = 6;
    private static final int LIST_W = 132;
    private static final int ROW_H = 17;
    private static final int CONTENT_X = 144;
    private static final int CONTENT_W = 274;
    private static final int HEADER_Y = 20;
    private static final int HEADER_H = 26;
    private static final int BODY_Y = 50;
    private static final int BODY_H = 190;
    private static final int RECIPE_CARD_H = 76;

    private static String rememberedId = "intro";

    private int left;
    private int top;
    private EditBox searchBox;
    private UiButton prevBtn;
    private UiButton nextBtn;

    private List<GuideRegistry.Chapter> allChapters = new ArrayList<>();
    private List<GuideRegistry.Chapter> filteredChapters = new ArrayList<>();
    private int selectedIdx = 0;
    private boolean recipesTab = false;
    private int listScroll = 0;
    private int textScroll = 0;
    private int recipeScroll = 0;
    private int tickCount = 0;

    private List<FormattedCharSequence> lines = new ArrayList<>();
    private List<RecipeEntry> chapterRecipes = new ArrayList<>();

    private record RecipeEntry(ItemStack result, String kindLabel, boolean cooking,
                               int width, int height, List<Ingredient> ingredients) {
    }

    public GuideScreen() {
        super(Component.translatable("gui.simple_bytestorage.guide.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - W) / 2;
        this.top = (this.height - H) / 2;
        this.allChapters = GuideRegistry.chapters();

        String prevSearch = this.searchBox != null ? this.searchBox.getValue() : "";
        this.searchBox = new EditBox(this.font, left + LIST_X + 3, top + 22, LIST_W - 6, 12, Component.empty());
        this.searchBox.setMaxLength(40);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(GuiStyle.TEXT_TITLE);
        this.searchBox.setHint(Component.translatable("gui.simple_bytestorage.guide.search")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setValue(prevSearch);
        this.searchBox.setResponder(q -> rebuildFilter());
        this.addRenderableWidget(this.searchBox);

        this.prevBtn = new UiButton(left + CONTENT_X + CONTENT_W - 36, top + HEADER_Y + 5, 16, 16,
                () -> Component.literal("<"), UiButton.Icon.NONE, () -> stepChapter(-1))
                .enabledWhen(() -> selectedIdx > 0);
        this.nextBtn = new UiButton(left + CONTENT_X + CONTENT_W - 18, top + HEADER_Y + 5, 16, 16,
                () -> Component.literal(">"), UiButton.Icon.NONE, () -> stepChapter(1))
                .enabledWhen(() -> selectedIdx + 1 < filteredChapters.size());

        rebuildFilter();
    }

    private void rebuildFilter() {
        String q = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        filteredChapters = new ArrayList<>();
        for (GuideRegistry.Chapter ch : allChapters) {
            if (q.isEmpty()) {
                filteredChapters.add(ch);
            } else {
                String title = Component.translatable(ch.titleKey()).getString().toLowerCase(Locale.ROOT);
                String body = Component.translatable(ch.bodyKey()).getString().toLowerCase(Locale.ROOT);
                if (title.contains(q) || body.contains(q) || ch.id().contains(q)) {
                    filteredChapters.add(ch);
                }
            }
        }
        selectedIdx = 0;
        for (int i = 0; i < filteredChapters.size(); i++) {
            if (filteredChapters.get(i).id().equals(rememberedId)) {
                selectedIdx = i;
                break;
            }
        }
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, filteredChapters.size() - listRows())));
        rebuildChapterContent();
    }

    private void stepChapter(int delta) {
        int next = selectedIdx + delta;
        if (next >= 0 && next < filteredChapters.size()) {
            select(next);
        }
    }

    private void select(int index) {
        if (index < 0 || index >= filteredChapters.size()) return;
        selectedIdx = index;
        rememberedId = filteredChapters.get(index).id();
        textScroll = 0;
        recipeScroll = 0;
        rebuildChapterContent();
        if (recipesTab && chapterRecipes.isEmpty()) {
            recipesTab = false;
        }
    }

    private void rebuildChapterContent() {
        lines = new ArrayList<>();
        chapterRecipes = new ArrayList<>();
        if (filteredChapters.isEmpty()) return;

        GuideRegistry.Chapter ch = filteredChapters.get(selectedIdx);
        String body = Component.translatable(ch.bodyKey()).getString();
        int wrapW = CONTENT_W - 18;
        for (String paragraph : body.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(this.font.split(FormattedText.of(paragraph), wrapW));
        }
        textScroll = Math.min(textScroll, maxTextScroll());
        loadChapterRecipes(ch);
        recipeScroll = Math.min(recipeScroll, maxRecipeScroll());
    }

    private void loadChapterRecipes(GuideRegistry.Chapter ch) {
        if (ch.recipeItems().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RecipeManager rm = mc.level.getRecipeManager();
        RegistryAccess reg = mc.level.registryAccess();

        Map<String, RecipeEntry> byItem = new LinkedHashMap<>();
        for (String itemId : ch.recipeItems()) {
            byItem.put(itemId, null);
        }

        for (Recipe<?> r : rm.getRecipes()) {
            ItemStack out = r.getResultItem(reg);
            if (out.isEmpty()) continue;
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(out.getItem());
            if (key == null) continue;
            String id = key.toString();
            if (!byItem.containsKey(id)) continue;

            // Предпочитаем верстак, но принимаем и плавку если верстачного рецепта нет
            RecipeEntry existing = byItem.get(id);
            if (r instanceof CraftingRecipe cr) {
                if (existing != null && !existing.cooking()) continue;
                int w = 3;
                int h = 3;
                NonNullList<Ingredient> ing = cr.getIngredients();
                if (cr instanceof ShapedRecipe sr) {
                    w = sr.getWidth();
                    h = sr.getHeight();
                }
                byItem.put(id, new RecipeEntry(out.copy(),
                        Component.translatable("gui.simple_bytestorage.guide.craft_table").getString(),
                        false, w, h, new ArrayList<>(ing)));
            } else if (r instanceof AbstractCookingRecipe acr && existing == null) {
                byItem.put(id, new RecipeEntry(out.copy(),
                        Component.translatable("gui.simple_bytestorage.guide.smelting").getString(),
                        true, 1, 1, new ArrayList<>(acr.getIngredients())));
            }
        }

        for (RecipeEntry entry : byItem.values()) {
            if (entry != null) {
                chapterRecipes.add(entry);
            }
        }
    }

    private static ItemStack resolveIcon(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            item = SimpleByteStorage.GUIDE_BOOK.get();
        }
        return new ItemStack(item);
    }

    private int listRows() {
        return (H - 46) / ROW_H;
    }

    private int visibleTextLines() {
        return (BODY_H - 12) / 10;
    }

    private int maxTextScroll() {
        int extra = chapterRecipes.isEmpty() ? 0 : 3;
        return Math.max(0, (lines.size() + extra) - visibleTextLines());
    }

    private int visibleRecipeCards() {
        return Math.max(1, (BODY_H - 8) / RECIPE_CARD_H);
    }

    private int maxRecipeScroll() {
        return Math.max(0, chapterRecipes.size() - visibleRecipeCards());
    }

    @Override
    public void tick() {
        super.tick();
        tickCount++;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (mx >= left + W - 14 && mx < left + W - 3 && my >= top + 4 && my < top + 14) {
                this.onClose();
                return true;
            }
            if (prevBtn.mouseClicked(mx, my, button) || nextBtn.mouseClicked(mx, my, button)) {
                return true;
            }
            // Вкладки Guide / Crafts внутри шапки
            int tabY = top + HEADER_Y + 5;
            int tab1X = left + CONTENT_X + CONTENT_W - 160;
            int tab2X = left + CONTENT_X + CONTENT_W - 110;
            if (my >= tabY && my < tabY + 16) {
                if (mx >= tab1X && mx < tab1X + 48) {
                    recipesTab = false;
                    return true;
                }
                if (!chapterRecipes.isEmpty() && mx >= tab2X && mx < tab2X + 70) {
                    recipesTab = true;
                    return true;
                }
            }
            // Клик по кнопке "View Recipes" внизу текста
            if (!recipesTab && !chapterRecipes.isEmpty()
                    && mx >= left + CONTENT_X + 8 && mx < left + CONTENT_X + CONTENT_W - 16
                    && my >= top + BODY_Y + BODY_H - 20 && my < top + BODY_Y + BODY_H - 4) {
                recipesTab = true;
                return true;
            }
            // Выбор главы слева
            int listTop = top + 38;
            if (mx >= left + LIST_X && mx < left + LIST_X + LIST_W - 6
                    && my >= listTop && my < listTop + listRows() * ROW_H) {
                int idx = listScroll + (int) ((my - listTop) / ROW_H);
                if (idx >= 0 && idx < filteredChapters.size()) {
                    select(idx);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int dir = (int) Math.signum(delta);
        if (mx < left + LIST_X + LIST_W) {
            int max = Math.max(0, filteredChapters.size() - listRows());
            listScroll = Math.max(0, Math.min(max, listScroll - dir));
        } else if (recipesTab) {
            recipeScroll = Math.max(0, Math.min(maxRecipeScroll(), recipeScroll - dir));
        } else {
            textScroll = Math.max(0, Math.min(maxTextScroll(), textScroll - dir * 3));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused() && keyCode != 256) {
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 266 || keyCode == 265) { // PageUp / Up
            if (recipesTab) {
                recipeScroll = Math.max(0, recipeScroll - 1);
            } else {
                textScroll = Math.max(0, textScroll - (keyCode == 266 ? visibleTextLines() : 1));
            }
            return true;
        }
        if (keyCode == 267 || keyCode == 264) { // PageDown / Down
            if (recipesTab) {
                recipeScroll = Math.min(maxRecipeScroll(), recipeScroll + 1);
            } else {
                textScroll = Math.min(maxTextScroll(), textScroll + (keyCode == 267 ? visibleTextLines() : 1));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g);
        GuiStyle.window(g, left, top, W, H, this.title, GuiStyle.LED_GOOD, "box");

        // --- Левая панель: поиск + оглавление с 3D-иконками ---
        GuiStyle.display(g, left + LIST_X, top + 20, LIST_W, 15);
        int listTop = top + 38;
        int listH = listRows() * ROW_H + 2;
        GuiStyle.display(g, left + LIST_X, listTop, LIST_W, listH);

        for (int row = 0; row < listRows(); row++) {
            int idx = listScroll + row;
            if (idx >= filteredChapters.size()) break;
            GuideRegistry.Chapter ch = filteredChapters.get(idx);
            int ry = listTop + 1 + row * ROW_H;
            boolean active = (idx == selectedIdx);
            boolean hover = mx >= left + LIST_X + 1 && mx < left + LIST_X + LIST_W - 7
                    && my >= ry && my < ry + ROW_H - 1;
            if (active) {
                g.fill(left + LIST_X + 1, ry, left + LIST_X + LIST_W - 7, ry + ROW_H - 1, GuiStyle.ACCENT_FAINT);
                g.fill(left + LIST_X + 1, ry, left + LIST_X + 3, ry + ROW_H - 1, GuiStyle.ACCENT);
            } else if (hover) {
                g.fill(left + LIST_X + 1, ry, left + LIST_X + LIST_W - 7, ry + ROW_H - 1, 0x22FFFFFF);
            }
            // Маленькая 3D-иконка главы (12x12)
            g.pose().pushPose();
            g.pose().translate(left + LIST_X + 5, ry + 2, 0);
            g.pose().scale(0.75F, 0.75F, 1.0F);
            g.renderItem(resolveIcon(ch.iconId()), 0, 0);
            g.pose().popPose();

            String chTitle = Component.translatable(ch.titleKey()).getString();
            g.drawString(this.font, GuiStyle.clip(this.font, chTitle, LIST_W - 28),
                    left + LIST_X + 20, ry + 4, active ? GuiStyle.ACCENT : GuiStyle.TEXT_TITLE, false);
        }
        GuiStyle.scrollbar(g, left + LIST_X + LIST_W - 6, listTop + 1, 5, listH - 2,
                listScroll, Math.max(0, filteredChapters.size() - listRows()), listRows(), filteredChapters.size());

        // --- Правая панель: шапка главы + тело ---
        GuiStyle.panel(g, left + CONTENT_X, top + HEADER_Y, CONTENT_W, HEADER_H);
        GuiStyle.display(g, left + CONTENT_X, top + BODY_Y, CONTENT_W, BODY_H);

        ItemStack hoveredRecipeStack = ItemStack.EMPTY;

        if (!filteredChapters.isEmpty()) {
            GuideRegistry.Chapter ch = filteredChapters.get(selectedIdx);
            // Иконка и название главы в шапке
            g.renderItem(resolveIcon(ch.iconId()), left + CONTENT_X + 5, top + HEADER_Y + 5);
            String titleStr = Component.translatable(ch.titleKey()).getString();
            g.drawString(this.font, GuiStyle.clip(this.font, titleStr, 88),
                    left + CONTENT_X + 24, top + HEADER_Y + 9, GuiStyle.ACCENT, false);

            // Вкладки Guide / Crafts (N)
            int tabY = top + HEADER_Y + 5;
            int tab1X = left + CONTENT_X + CONTENT_W - 160;
            int tab2X = left + CONTENT_X + CONTENT_W - 110;
            drawMiniTab(g, tab1X, tabY, 48,
                    Component.translatable("gui.simple_bytestorage.guide.tab.guide").getString(),
                    !recipesTab, true, mx, my);
            String craftTabTxt = Component.translatable("gui.simple_bytestorage.guide.tab.crafts").getString()
                    + " (" + chapterRecipes.size() + ")";
            drawMiniTab(g, tab2X, tabY, 70, craftTabTxt,
                    recipesTab, !chapterRecipes.isEmpty(), mx, my);

            if (recipesTab) {
                hoveredRecipeStack = renderRecipesTab(g, mx, my);
            } else {
                renderTextTab(g, mx, my);
            }
        } else {
            g.drawString(this.font, Component.translatable("gui.simple_bytestorage.guide.no_results"),
                    left + CONTENT_X + 8, top + BODY_Y + 8, GuiStyle.TEXT_MUTED, false);
        }

        prevBtn.render(g, this.font, mx, my);
        nextBtn.render(g, this.font, mx, my);
        super.render(g, mx, my, partialTick);

        if (!hoveredRecipeStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredRecipeStack, mx, my);
        }
    }

    private void drawMiniTab(GuiGraphics g, int x, int y, int w, String text,
                             boolean active, boolean enabled, int mx, int my) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + 16;
        int bg = active ? GuiStyle.ACCENT_DIM : (hover ? GuiStyle.ACCENT_FAINT : GuiStyle.PANEL_BG);
        g.fill(x, y, x + w, y + 16, bg);
        g.fill(x, y + 15, x + w, y + 16, active ? GuiStyle.ACCENT : GuiStyle.PANEL_EDGE_LIGHT);
        int col = !enabled ? GuiStyle.TEXT_MUTED : (active ? GuiStyle.TEXT_TITLE : GuiStyle.TEXT_LABEL);
        String clipped = GuiStyle.clip(this.font, text, w - 6);
        g.drawString(this.font, clipped, x + (w - this.font.width(clipped)) / 2, y + 4, col, false);
    }

    private void renderTextTab(GuiGraphics g, int mx, int my) {
        int x = left + CONTENT_X + 8;
        int y = top + BODY_Y + 6;
        int maxVis = chapterRecipes.isEmpty() ? visibleTextLines() : visibleTextLines() - 2;
        for (int i = textScroll; i < Math.min(lines.size(), textScroll + maxVis); i++) {
            g.drawString(this.font, lines.get(i), x, y, GuiStyle.TEXT_TITLE, false);
            y += 10;
        }
        GuiStyle.scrollbar(g, left + CONTENT_X + CONTENT_W - 7, top + BODY_Y + 2, 5, BODY_H - 4,
                textScroll, maxTextScroll(), maxVis, lines.size());

        // Нижняя кнопка-подсказка «Смотреть рецепты (N) ->»
        if (!chapterRecipes.isEmpty()) {
            int bx = left + CONTENT_X + 8;
            int by = top + BODY_Y + BODY_H - 20;
            int bw = CONTENT_W - 22;
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + 15;
            g.fill(bx, by, bx + bw, by + 15, hover ? GuiStyle.ACCENT_DIM : GuiStyle.ACCENT_FAINT);
            g.fill(bx, by, bx + 2, by + 15, GuiStyle.ACCENT);
            String label = Component.translatable("gui.simple_bytestorage.guide.view_crafts", chapterRecipes.size()).getString();
            g.drawString(this.font, GuiStyle.clip(this.font, label, bw - 10), bx + 6, by + 4, GuiStyle.ACCENT, false);
        }
    }

    private ItemStack renderRecipesTab(GuiGraphics g, int mx, int my) {
        ItemStack hovered = ItemStack.EMPTY;
        int vis = visibleRecipeCards();
        int cardX = left + CONTENT_X + 6;
        int cardW = CONTENT_W - 18;

        for (int i = 0; i < vis; i++) {
            int idx = recipeScroll + i;
            if (idx >= chapterRecipes.size()) break;
            RecipeEntry rec = chapterRecipes.get(idx);
            int cy = top + BODY_Y + 4 + i * RECIPE_CARD_H;

            GuiStyle.panel(g, cardX, cy, cardW, RECIPE_CARD_H - 4);
            // Заголовок карточки: название предмета + бейдж типа рецепта
            String name = rec.result().getHoverName().getString();
            int badgeW = this.font.width(rec.kindLabel()) + 6;
            g.drawString(this.font, GuiStyle.clip(this.font, name, cardW - badgeW - 12),
                    cardX + 6, cy + 4, GuiStyle.TEXT_TITLE, false);
            g.drawString(this.font, rec.kindLabel(), cardX + cardW - badgeW, cy + 4, GuiStyle.TEXT_MUTED, false);

            int gridX = cardX + 10;
            int gridY = cy + 16;

            if (rec.cooking()) {
                // Печь / плавильня: 1 входной слот + печь + стрелка -> результат
                GuiStyle.slot(g, gridX + 18, gridY + 18, GuiStyle.SLOT_POOL);
                ItemStack in = pickIngredient(rec.ingredients().isEmpty() ? Ingredient.EMPTY : rec.ingredients().get(0));
                if (!in.isEmpty()) {
                    g.renderItem(in, gridX + 19, gridY + 19);
                    if (inSlot(mx, my, gridX + 18, gridY + 18)) hovered = in;
                }
                g.renderItem(new ItemStack(Items.FURNACE), gridX + 50, gridY + 19);
                g.drawString(this.font, "->", gridX + 76, gridY + 23, GuiStyle.ACCENT, false);
                int outX = gridX + 98;
                int outY = gridY + 18;
                GuiStyle.slot(g, outX, outY, GuiStyle.SLOT_CARD);
                g.renderItem(rec.result(), outX + 1, outY + 1);
                g.renderItemDecorations(this.font, rec.result(), outX + 1, outY + 1);
                if (inSlot(mx, my, outX, outY)) hovered = rec.result();
            } else {
                // Сетка верстака 3x3
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int sx = gridX + c * 18;
                        int sy = gridY + r * 18;
                        GuiStyle.slot(g, sx, sy, GuiStyle.SLOT_POOL);
                        int ingIdx = (c < rec.width() && r < rec.height()) ? (r * rec.width() + c) : -1;
                        if (ingIdx >= 0 && ingIdx < rec.ingredients().size()) {
                            ItemStack st = pickIngredient(rec.ingredients().get(ingIdx));
                            if (!st.isEmpty()) {
                                g.renderItem(st, sx + 1, sy + 1);
                                if (inSlot(mx, my, sx, sy)) hovered = st;
                            }
                        }
                    }
                }
                // Стрелка и слот результата
                int arrowX = gridX + 64;
                int arrowY = gridY + 23;
                g.drawString(this.font, "-->", arrowX, arrowY, GuiStyle.ACCENT, false);
                int outX = gridX + 92;
                int outY = gridY + 18;
                GuiStyle.slot(g, outX, outY, GuiStyle.SLOT_CARD);
                g.renderItem(rec.result(), outX + 1, outY + 1);
                g.renderItemDecorations(this.font, rec.result(), outX + 1, outY + 1);
                if (inSlot(mx, my, outX, outY)) hovered = rec.result();

                // Подпись количества справа от результата
                if (rec.result().getCount() > 1) {
                    g.drawString(this.font, "x" + rec.result().getCount(), outX + 24, outY + 5, GuiStyle.TEXT_GOOD, false);
                }
            }
        }

        GuiStyle.scrollbar(g, left + CONTENT_X + CONTENT_W - 7, top + BODY_Y + 2, 5, BODY_H - 4,
                recipeScroll, maxRecipeScroll(), vis, chapterRecipes.size());
        return hovered;
    }

    private ItemStack pickIngredient(Ingredient ing) {
        if (ing == null || ing.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        int idx = (tickCount / 20) % items.length;
        return items[idx];
    }

    private static boolean inSlot(int mx, int my, int sx, int sy) {
        return mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18;
    }
}
