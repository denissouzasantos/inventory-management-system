package com.example.inventory.service;

import com.example.inventory.event.StockAdjusted;
import com.example.inventory.event.StockReplaced;
import com.example.inventory.model.InventoryRecord;
import org.springframework.stereotype.Service;
import io.micrometer.observation.annotation.Observed;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoreInventoryService {
    private final InMemoryEventBus eventBus;
    // Key: storeId|sku
    private final Map<String, InventoryRecord> storeState = new ConcurrentHashMap<>();

    public StoreInventoryService(InMemoryEventBus eventBus) {
        this.eventBus = eventBus;
    }

    private String key(String storeId, String sku) {
        return storeId + "|" + sku;
    }

    @Observed(name = "inventory.service.replace")
    public InventoryRecord replaceStock(String storeId, String sku, int quantity) {
        Objects.requireNonNull(storeId);
        Objects.requireNonNull(sku);
        if (quantity < 0) throw new IllegalArgumentException("quantity must be >= 0");
        InventoryRecord next = storeState.compute(key(storeId, sku), (k, current) -> {
            long nextVersion = current == null ? 1 : current.getVersion() + 1;
            return new InventoryRecord(storeId, sku, quantity, nextVersion, Instant.now());
        });
        eventBus.publish(new StockReplaced(storeId, sku, next.getQuantity(), next.getVersion(), next.getUpdatedAt()));
        return next;
    }

    @Observed(name = "inventory.service.adjust")
    public InventoryRecord adjustStock(String storeId, String sku, int delta) {
        Objects.requireNonNull(storeId);
        Objects.requireNonNull(sku);
        InventoryRecord next = storeState.compute(key(storeId, sku), (k, current) -> {
            int currentQty = current == null ? 0 : current.getQuantity();
            long nextVersion = current == null ? 1 : current.getVersion() + 1;
            int newQty = Math.max(0, currentQty + delta);
            return new InventoryRecord(storeId, sku, newQty, nextVersion, Instant.now());
        });
        eventBus.publish(new StockAdjusted(storeId, sku, delta, next.getVersion(), next.getUpdatedAt()));
        return next;
    }

    public Optional<InventoryRecord> getStock(String storeId, String sku) {
        return Optional.ofNullable(storeState.get(key(storeId, sku)));
    }
}

