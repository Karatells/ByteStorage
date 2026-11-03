package com.simpleae.bytestorage.network;

import com.simpleae.bytestorage.api.network.GridState;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.network.IGridNode;
import com.simpleae.bytestorage.api.storage.IDriveHost;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.api.storage.IPatternStorage;
import com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity;
import com.simpleae.bytestorage.storage.ByteStorageHandler;
import com.simpleae.bytestorage.storage.PatternStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Базова реалізація мережі Simple Byte Storage.
 * Відповідає за збір нод, підрахунок енергії, виявлення конфліктів
 * та управління спільним пулом зберігання (ByteStorageHandler).
 */
public class GridNetwork implements IGrid {

    private final UUID gridId;
    private NetworkControllerBlockEntity controller;
    private final Set<IGridNode> nodes = new HashSet<>();
    private final INetworkStorage storage;
    private final IPatternStorage patternStorage;
    private GridState state = GridState.OFFLINE;

    public GridNetwork(NetworkControllerBlockEntity controller) {
        this.gridId = UUID.randomUUID();
        this.controller = controller;
        this.storage = new ByteStorageHandler();
        PatternStorage patterns = new PatternStorage();
        // Будь-яка зміна списку шаблонів позначає контролер «брудним», щоб шаблони потрапили у збереження світу.
        patterns.setChangeListener(() -> {
            NetworkControllerBlockEntity c = this.controller;
            if (c != null) {
                c.setChanged();
            }
        });
        this.patternStorage = patterns;
        this.addNode(controller);
    }

    @Override
    public UUID getGridId() {
        return gridId;
    }

    @Override
    public GridState getState() {
        return state;
    }

    @Override
    public void setState(GridState newState) {
        if (this.state != newState) {
            this.state = newState;
            for (IGridNode node : nodes) {
                node.onGridStateChanged(newState);
            }
        }
    }

    @Override
    @Nullable
    public NetworkControllerBlockEntity getController() {
        return controller;
    }

    @Override
    public INetworkStorage getStorage() {
        return storage;
    }

    @Override
    public IPatternStorage getPatternStorage() {
        return patternStorage;
    }

    @Override
    public Collection<IGridNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Override
    public void addNode(IGridNode node) {
        if (nodes.add(node)) {
            node.setGrid(this);

            // Якщо нода є блоком-накопичувачем дисків, реєструємо її у сховищі
            if (node instanceof IDriveHost drive) {
                storage.attachDrive(drive);
            }

            validateNetwork();
        }
    }

    @Override
    public void removeNode(IGridNode node) {
        if (nodes.remove(node)) {
            node.setGrid(null);

            if (node instanceof IDriveHost drive) {
                storage.detachDrive(drive);
            }

            if (node == controller) {
                this.controller = null;
                setState(GridState.OFFLINE);
            } else {
                validateNetwork();
            }
        }
    }

    @Override
    public int getTotalEnergyDrain() {
        if (state == GridState.CONFLICT) {
            return 0;
        }

        int drain = 10; // Базове споживання контролера (FE/t)
        for (IGridNode node : nodes) {
            if (!node.isController()) {
                drain += node.getIdleEnergyDrain();
            }
        }
        return drain;
    }

    @Override
    public void validateNetwork() {
        int controllerCount = 0;
        for (IGridNode node : nodes) {
            if (node.isController()) {
                controllerCount++;
            }
        }

        if (controllerCount > 1) {
            setState(GridState.CONFLICT);
        } else if (controller == null || !controller.hasEnoughEnergy()) {
            setState(GridState.OFFLINE);
        } else {
            setState(GridState.ONLINE);
        }
    }
}
