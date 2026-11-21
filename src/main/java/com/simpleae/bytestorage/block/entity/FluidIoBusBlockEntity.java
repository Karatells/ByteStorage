package com.simpleae.bytestorage.block.entity;

import com.simpleae.bytestorage.SimpleByteStorage;
import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.item.FluidCellItem;
import com.simpleae.bytestorage.menu.FluidIoBusMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** BlockEntity Рідинної I/O-шини. */
public class FluidIoBusBlockEntity extends BlockEntity implements IGridNode, MenuProvider {

    private static final int CYCLE_TICKS = 10;
    private static final int MB_PER_CYCLE = 1000;

    private IGrid grid;
    private boolean exportMode = false;
    private int tickCounter = 0;

    public FluidIoBusBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleByteStorage.FLUID_IO_BUS_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isExportMode() {
        return exportMode;
    }

    public void toggleExportMode() {
        exportMode = !exportMode;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidIoBusBlockEntity bus) {
        if (++bus.tickCounter % CYCLE_TICKS != 0) {
            return;
        }
        IGrid grid = bus.grid;
        if (grid == null || !grid.getState().isActive() || grid.isEcoMode()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (!level.isLoaded(adj)) continue;
            BlockEntity be = level.getBlockEntity(adj);
            if (be == null || be instanceof IGridNode) continue;
            be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                if (bus.exportMode) {
                    bus.exportFluid(grid, handler);
                } else {
                    bus.importFluid(grid, handler);
                }
            });
        }
    }

    private void importFluid(IGrid grid, IFluidHandler handler) {
        FluidStack drained = handler.drain(MB_PER_CYCLE, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int remaining = drained.getAmount();
        for (IGridNode node : grid.getNodes()) {
            if (!(node instanceof DriveBlockEntity drive) || drive.isLocked()) continue;
            IItemHandler inv = drive.getDriveInventory();
            for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
                ItemStack cell = inv.getStackInSlot(i);
                if (cell.getItem() instanceof FluidCellItem fluidCell) {
                    FluidStack sim = drained.copy();
                    sim.setAmount(remaining);
                    int fit = fluidCell.insertFluid(cell, sim, true);
                    if (fit > 0) {
                        FluidStack taken = handler.drain(fit, IFluidHandler.FluidAction.EXECUTE);
                        if (!taken.isEmpty()) {
                            fluidCell.insertFluid(cell, taken, false);
                            drive.setChanged();
                            remaining -= taken.getAmount();
                        }
                    }
                }
            }
        }
    }

    private void exportFluid(IGrid grid, IFluidHandler handler) {
        for (IGridNode node : grid.getNodes()) {
            if (!(node instanceof DriveBlockEntity drive)) continue;
            IItemHandler inv = drive.getDriveInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack cell = inv.getStackInSlot(i);
                if (cell.getItem() instanceof FluidCellItem fluidCell) {
                    FluidStack sim = fluidCell.extractFluid(cell, MB_PER_CYCLE, true);
                    if (!sim.isEmpty()) {
                        int accepted = handler.fill(sim, IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0) {
                            fluidCell.extractFluid(cell, accepted, false);
                            drive.setChanged();
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    @Nullable
    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void setGrid(@Nullable IGrid grid) {
        this.grid = grid;
    }

    @Override
    public BlockPos getNodePos() {
        return this.worldPosition;
    }

    @Override
    public Level getNodeLevel() {
        return this.level;
    }

    @Override
    public int getIdleEnergyDrain() {
        return 2;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simple_bytestorage.fluid_io_bus");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FluidIoBusMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Export", exportMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        exportMode = tag.getBoolean("Export");
    }
}
