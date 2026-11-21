package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.api.storage.CellTier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Рідинна байт-ячейка: зберігає рідини за правилом 1000 mB = 1 байт без ліміту на типи рідин.
 */
public class FluidCellItem extends Item {

    public static final String TAG_FLUIDS = "FluidInventory";
    public static final int MB_PER_BYTE = 1000;
    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private final CellTier tier;

    public FluidCellItem(CellTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public CellTier getTier() {
        return tier;
    }

    public int getMaxMb() {
        return tier.getCapacityBytes() * MB_PER_BYTE;
    }

    public static List<FluidStack> getStoredFluids(ItemStack cell) {
        List<FluidStack> list = new ArrayList<>();
        CompoundTag tag = cell.getTag();
        if (tag == null || !tag.contains(TAG_FLUIDS, Tag.TAG_LIST)) {
            return list;
        }
        ListTag lt = tag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < lt.size(); i++) {
            FluidStack fs = FluidStack.loadFluidStackFromNBT(lt.getCompound(i));
            if (!fs.isEmpty()) {
                list.add(fs);
            }
        }
        return list;
    }

    private static void saveFluids(ItemStack cell, List<FluidStack> fluids) {
        ListTag lt = new ListTag();
        for (FluidStack fs : fluids) {
            if (!fs.isEmpty()) {
                lt.add(fs.writeToNBT(new CompoundTag()));
            }
        }
        cell.getOrCreateTag().put(TAG_FLUIDS, lt);
    }

    public int getUsedMb(ItemStack cell) {
        int sum = 0;
        for (FluidStack fs : getStoredFluids(cell)) {
            sum += fs.getAmount();
        }
        return sum;
    }

    public int insertFluid(ItemStack cell, FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;
        int free = Math.max(0, getMaxMb() - getUsedMb(cell));
        int accepted = Math.min(free, resource.getAmount());
        if (accepted <= 0) return 0;
        if (!simulate) {
            List<FluidStack> fluids = getStoredFluids(cell);
            boolean merged = false;
            for (FluidStack fs : fluids) {
                if (fs.isFluidEqual(resource)) {
                    fs.grow(accepted);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                FluidStack copy = resource.copy();
                copy.setAmount(accepted);
                fluids.add(copy);
            }
            saveFluids(cell, fluids);
        }
        return accepted;
    }

    public FluidStack extractFluid(ItemStack cell, int maxDrain, boolean simulate) {
        if (maxDrain <= 0) return FluidStack.EMPTY;
        List<FluidStack> fluids = getStoredFluids(cell);
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fs = fluids.get(i);
            if (!fs.isEmpty()) {
                int take = Math.min(maxDrain, fs.getAmount());
                FluidStack out = fs.copy();
                out.setAmount(take);
                if (!simulate) {
                    fs.shrink(take);
                    if (fs.isEmpty()) fluids.remove(i);
                    saveFluids(cell, fluids);
                }
                return out;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getUsedMb(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float ratio = (float) getUsedMb(stack) / Math.max(1, getMaxMb());
        return Math.round(13.0F * Mth.clamp(ratio, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x0096FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.fluid_cell",
                FORMAT.format(getUsedMb(stack)), FORMAT.format(getMaxMb()))
                .withStyle(ChatFormatting.AQUA));
    }
}
