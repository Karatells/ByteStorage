package com.simpleae.bytestorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Улучшение Беспроводного терминала (карта). Каждый вид можно установить только один раз. */
public class WirelessUpgradeItem extends Item {

    public enum Type {
        AUTO_TRANSFER("auto_transfer"),
        INSURANCE("insurance"),
        TOOL("tool"),
        MACRO("macro");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String nameKey() {
            return "gui.simple_bytestorage.wireless.upgrade." + id;
        }

        public String descKey() {
            return "gui.simple_bytestorage.wireless.upgrade." + id + ".desc";
        }
    }

    private final Type type;

    public WirelessUpgradeItem(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(type.descKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.upgrade.install").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
