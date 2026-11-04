package com.simpleae.bytestorage.item;

import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.menu.TerminalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Бездротовий термінал. Прив'язується і заряджається, коли лежить у слоті Контролера мережі.
 * ПКМ у повітрі — відкрити прив'язаний Термінал на відстані (потрібен заряд FE; працює, поки чанк завантажено).
 */
public class WirelessTerminalItem extends Item {

    public static final int CAPACITY = 20_000;
    public static final int COST_PER_OPEN = 100;
    public static final int CHARGE_PER_TICK = 50;

    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_DIM = "BoundDim";
    private static final String TAG_POS = "BoundPos";

    public WirelessTerminalItem(Properties properties) {
        super(properties);
    }

    // --- Енергія ---

    public static int getEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_ENERGY);
    }

    public static void setEnergy(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt(TAG_ENERGY, Math.max(0, Math.min(CAPACITY, energy)));
    }

    private static final class ItemEnergy implements IEnergyStorage {
        private final ItemStack stack;

        ItemEnergy(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.max(0, Math.min(CAPACITY - getEnergy(stack), maxReceive));
            if (!simulate && accepted > 0) {
                setEnergy(stack, getEnergy(stack) + accepted);
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return getEnergy(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new ItemEnergy(stack));
        return new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
                return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00D8F0;
    }

    // --- Прив'язка ---

    public static boolean isBound(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_DIM) && tag.contains(TAG_POS);
    }

    public static BlockPos boundPos(ItemStack stack) {
        return BlockPos.of(stack.getOrCreateTag().getLong(TAG_POS));
    }

    /** Привязать терминал к контроллеру (вызывается слотом контроллера). */
    public static void bind(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DIM, level.dimension().location().toString());
        tag.putLong(TAG_POS, pos.asLong());
    }

    /** Зарядить терминал; возвращает принятое количество FE. */
    public static int charge(ItemStack stack, int amount) {
        int accepted = Math.max(0, Math.min(CAPACITY - getEnergy(stack), amount));
        if (accepted > 0) {
            setEnergy(stack, getEnergy(stack) + accepted);
        }
        return accepted;
    }

    /** Контроллер, к которому привязан терминал, если он доступен на сервере. */
    @Nullable
    public static NetworkControllerBlockEntity resolveController(ItemStack stack, net.minecraft.server.MinecraftServer server) {
        if (!isBound(stack) || server == null) return null;
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(stack.getOrCreateTag().getString(TAG_DIM)));
        ServerLevel target = server.getLevel(dimension);
        BlockPos pos = boundPos(stack);
        if (target == null || !target.isLoaded(pos)) return null;
        return target.getBlockEntity(pos) instanceof NetworkControllerBlockEntity c ? c : null;
    }

    // --- Відкриття ---

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, p) -> new com.simpleae.bytestorage.menu.WirelessSettingsMenu(id, inventory, slot),
                            Component.translatable("gui.simple_bytestorage.wireless.settings")),
                    buf -> buf.writeVarInt(slot));
            return InteractionResultHolder.consume(stack);
        }
        if (!isBound(stack)) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.unbound"), true);
            return InteractionResultHolder.fail(stack);
        }
        NetworkControllerBlockEntity controller = resolveController(stack, serverPlayer.server);
        if (controller == null) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.unreachable"), true);
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = controller.getBlockPos();
        if (controller.getGrid() == null || !controller.getGrid().getState().isActive()) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.no_network"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (getEnergy(stack) < COST_PER_OPEN) {
            player.displayClientMessage(Component.translatable("msg.simple_bytestorage.wireless.no_energy"), true);
            return InteractionResultHolder.fail(stack);
        }
        setEnergy(stack, getEnergy(stack) - COST_PER_OPEN);
        NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, p) -> new TerminalMenu(id, inventory, controller, true),
                        Component.translatable("block.simple_bytestorage.terminal")),
                buf -> buf.writeBlockPos(pos));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.simple_bytestorage.wireless.energy",
                getEnergy(stack), CAPACITY).withStyle(ChatFormatting.AQUA));
        if (isBound(stack)) {
            BlockPos pos = boundPos(stack);
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.wireless.bound",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.simple_bytestorage.wireless.help")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
