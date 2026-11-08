package com.simpleae.bytestorage.scripting.block.entity;

import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.scripting.SimpleAeScripting;
import com.simpleae.bytestorage.scripting.script.BusContext;
import com.simpleae.bytestorage.scripting.script.LogicRuntime;
import com.simpleae.bytestorage.scripting.script.UiElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Логічна шина: виконує JavaScript-скрипт, що переміщує предмети між сусідніми інвентарями та мережею.
 * Скрипт редагується через Логічний термінал.
 */
public class LogicBusBlockEntity extends BlockEntity implements IGridNode, BusContext {

    public static final String DEFAULT_SCRIPT = String.join("\n",
            "// Логічна шина. Верхній рівень виконується один раз,",
            "// tick() — кожні N тіків (інтервал задається в терміналі).",
            "// Приклад: забирати залізні злитки зі скрині зверху в мережу.",
            "var chest = side(\"up\");",
            "",
            "function tick() {",
            "  if (chest) {",
            "    chest.pull(\"minecraft:iron_ingot\");",
            "  }",
            "}",
            "");

    private static final int MAX_LOG = 16;

    private IGrid grid;
    private String script = DEFAULT_SCRIPT;
    private boolean enabled = false;
    private int interval = 20;
    private String identifier = "";
    private String status = "";
    private int counter = 0;
    private boolean dirty = true;
    private LogicRuntime runtime;
    private final Deque<String> log = new ArrayDeque<>();
    private List<UiElement> panel = List.of();
    private final Deque<String> buttons = new ArrayDeque<>();

    public LogicBusBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleAeScripting.LOGIC_BUS_ENTITY.get(), pos, state);
    }

    // --- IGridNode ---

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
        return enabled ? 4 : 1;
    }

    @Override
    public void onGridStateChanged(GridState newState) {
        setChanged();
    }

    // --- BusContext ---

    @Override
    public IGrid grid() {
        return grid;
    }

    @Override
    public Level level() {
        return level;
    }

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public List<BusContext> busesWithId(String id) {
        List<LogicBusBlockEntity> found = new ArrayList<>();
        if (grid != null && id != null && !id.isEmpty()) {
            for (IGridNode node : grid.getNodes()) {
                if (node instanceof LogicBusBlockEntity bus && !bus.isRemoved()
                        && bus.getLevel() == level && id.equals(bus.identifier)) {
                    found.add(bus);
                }
            }
        }
        found.sort(java.util.Comparator.comparingLong((LogicBusBlockEntity b) -> b.getBlockPos().asLong()));
        return new ArrayList<>(found);
    }

    @Override
    public void log(String message) {
        String line = message.length() > 160 ? message.substring(0, 160) : message;
        log.addLast(line);
        while (log.size() > MAX_LOG) {
            log.removeFirst();
        }
    }

    @Override
    public void setPanel(List<UiElement> elements) {
        this.panel = List.copyOf(elements);
    }

    @Override
    public List<String> takeButtons() {
        List<String> taken = new ArrayList<>(buttons);
        buttons.clear();
        return taken;
    }

    /** Нажатие кнопки панели в терминале: скрипт получит его при следующем запуске (onButton). */
    public void pressButton(String id) {
        if (enabled && buttons.size() < 8 && !id.isEmpty()) {
            buttons.addLast(id.length() > UiElement.MAX_ID ? id.substring(0, UiElement.MAX_ID) : id);
        }
    }

    public List<UiElement> getPanel() {
        return panel;
    }

    // --- Стан для терміналу ---

    public String getScript() {
        return script;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getInterval() {
        return interval;
    }

    public String getIdentifier() {
        return identifier;
    }

    /** Допустимі символи: літери, цифри, _ - . (до 32). Решта прибирається. */
    public static String sanitizeIdentifier(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.trim().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.') {
                sb.append(c);
                if (sb.length() >= 32) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    public void setIdentifier(String raw) {
        this.identifier = sanitizeIdentifier(raw);
        setChanged();
    }

    public String getStatus() {
        return status;
    }

    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    public void setScript(String text) {
        int max = ScriptingConfig.MAX_SCRIPT_LENGTH.get();
        this.script = text.length() > max ? text.substring(0, max) : text;
        this.dirty = true;
        this.enabled = false;
        this.status = "saved";
        this.panel = List.of();
        this.buttons.clear();
        setChanged();
    }

    public void setEnabled(boolean value) {
        if (value && !enabled) {
            dirty = true; // свіжий запуск: перекомпілювати та скинути помилку
            status = "";
        }
        this.enabled = value;
        if (!value) {
            buttons.clear();
        }
        if (!value && status.isEmpty()) {
            status = "stopped";
        }
        setChanged();
    }

    public void setInterval(int ticks) {
        this.interval = Math.max(ScriptingConfig.MIN_INTERVAL.get(), Math.min(1200, ticks));
        setChanged();
    }

    // --- Тік ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, LogicBusBlockEntity bus) {
        bus.tick();
    }

    private void tick() {
        if (level == null || level.isClientSide || !enabled || !ScriptingConfig.ENABLED.get()) {
            return;
        }
        if (grid == null || !grid.getState().isActive()) {
            return;
        }
        int step = Math.max(interval, ScriptingConfig.MIN_INTERVAL.get());
        if (++counter < step) {
            return;
        }
        counter = 0;

        if (runtime == null || dirty) {
            runtime = new LogicRuntime(this);
            dirty = false;
            String error = runtime.compile(script);
            if (error != null) {
                fail(error);
                return;
            }
            status = "running";
        }
        String error = runtime.tick();
        if (error != null) {
            fail(error);
        }
    }

    /** Выполнить скрипт один раз прямо сейчас (макрос Беспроводного терминала), независимо от включения и интервала. */
    public boolean runNow() {
        if (level == null || level.isClientSide || !ScriptingConfig.ENABLED.get()) {
            return false;
        }
        if (grid == null || !grid.getState().isActive()) {
            return false;
        }
        if (runtime == null || dirty) {
            runtime = new LogicRuntime(this);
            dirty = false;
            String error = runtime.compile(script);
            if (error != null) {
                fail(error);
                return false;
            }
        }
        String error = runtime.tick();
        if (error != null) {
            fail(error);
            return false;
        }
        return true;
    }

    private void fail(String error) {
        enabled = false;
        status = "ERROR: " + error;
        log("ERROR: " + error);
        setChanged();
    }

    // --- Збереження ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Script", script);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("Interval", interval);
        tag.putString("Id", identifier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Script")) {
            script = tag.getString("Script");
        }
        enabled = tag.getBoolean("Enabled");
        identifier = sanitizeIdentifier(tag.getString("Id"));
        interval = tag.contains("Interval") ? tag.getInt("Interval") : 20;
        dirty = true;
        runtime = null;
        status = enabled ? "running" : "";
    }
}
