package com.simpleae.bytestorage.network.packet;

import com.simpleae.bytestorage.menu.PatternTerminalMenu;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * План замовлення (S2C) для вікна підтвердження: що буде скрафчено (по кроках) і чого не вистачає.
 */
public class CraftPlanPacket {

    /** Рядок плану: предмет, кількість (крафтів для кроку / штук для бракуючого) і тип шаблону. */
    public static final class Entry {
        public final ItemStack stack;
        public final int count;
        public final boolean processing;

        public Entry(ItemStack stack, int count, boolean processing) {
            this.stack = stack;
            this.count = count;
            this.processing = processing;
        }
    }

    private final String patternId;
    private final int amount;
    private final List<Entry> steps;
    private final List<Entry> missing;

    public CraftPlanPacket(String patternId, int amount, List<Entry> steps, List<Entry> missing) {
        this.patternId = patternId;
        this.amount = amount;
        this.steps = steps;
        this.missing = missing;
    }

    public static CraftPlanPacket of(String patternId, int amount, CraftingService.Plan plan) {
        List<Entry> steps = new ArrayList<>();
        for (CraftingService.Step step : plan.steps) {
            steps.add(new Entry(step.pattern.getOutput(), step.crafts, step.pattern.isProcessing()));
        }
        List<Entry> missing = new ArrayList<>();
        for (ItemStack stack : plan.missing) {
            missing.add(new Entry(stack, stack.getCount(), false));
        }
        return new CraftPlanPacket(patternId, amount, steps, missing);
    }

    public CraftPlanPacket(FriendlyByteBuf buf) {
        this.patternId = buf.readUtf(128);
        this.amount = buf.readVarInt();
        this.steps = readEntries(buf);
        this.missing = readEntries(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(patternId, 128);
        buf.writeVarInt(amount);
        writeEntries(buf, steps);
        writeEntries(buf, missing);
    }

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> entries) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            ItemStack one = entry.stack.copy();
            one.setCount(1);
            buf.writeItem(one);
            buf.writeVarInt(entry.count);
            buf.writeBoolean(entry.processing);
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItem();
            int count = buf.readVarInt();
            boolean processing = buf.readBoolean();
            list.add(new Entry(stack, count, processing));
        }
        return list;
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.containerMenu instanceof PatternTerminalMenu menu) {
                menu.setClientPlan(patternId, amount, steps, missing);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
