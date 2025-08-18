package com.example.inventory.service;

import com.example.inventory.event.InventoryEvent;
import com.example.inventory.event.StockAdjusted;
import com.example.inventory.event.StockReplaced;
import com.example.inventory.model.InventoryRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CentralInventoryProjection {
    // Key: sku -> total quantity across stores
    private final Map<String, Integer> globalQuantityBySku = new ConcurrentHashMap<>();
    // Last known version per store+sku to implement last-write-wins per partition
    private final Map<String, Long> versionByStoreSku = new ConcurrentHashMap<>();

    private final InMemoryEventBus eventBus;

    public CentralInventoryProjection(InMemoryEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(this::onEvent);
    }

    private void onEvent(InventoryEvent event) {
        String storeSku = event.storeId() + "|" + event.sku();
        long currentVersion = versionByStoreSku.getOrDefault(storeSku, 0L);
        if (event.version() <= currentVersion) {
            return; // ignore stale event
        }
        versionByStoreSku.put(storeSku, event.version());

        if (event instanceof StockReplaced replaced) {
            applyReplace(replaced);
        } else if (event instanceof StockAdjusted adjusted) {
            applyAdjust(adjusted);
        }
    }

    private void applyReplace(StockReplaced e) {
        String sku = e.sku();
        int previous = globalQuantityBySku.getOrDefault(sku, 0);
        // The event does not include the prior store-level quantity; for simplicity,
        // we treat replace as idempotent by tracking per-store latest quantity.
        // Maintain a per-store snapshot to compute total accurately.
        // For prototype: we keep an auxiliary map of store quantities per sku.
        perStoreQuantities.compute(e.storeId() + "|" + sku, (k, oldQty) -> e.quantity());
        int newTotal = perStoreQuantities.entrySet().stream()
            .filter(en -> en.getKey().endsWith("|" + sku))
            .mapToInt(Map.Entry::getValue)
            .sum();
        globalQuantityBySku.put(sku, newTotal);
    }

    private final Map<String, Integer> perStoreQuantities = new ConcurrentHashMap<>();

    private void applyAdjust(StockAdjusted e) {
        String storeSku = e.storeId() + "|" + e.sku();
        int currentStoreQty = Math.max(0, perStoreQuantities.getOrDefault(storeSku, 0) + e.delta());
        perStoreQuantities.put(storeSku, currentStoreQty);
        int newTotal = perStoreQuantities.entrySet().stream()
            .filter(en -> en.getKey().endsWith("|" + e.sku()))
            .mapToInt(Map.Entry::getValue)
            .sum();
        globalQuantityBySku.put(e.sku(), newTotal);
    }

    public int getGlobalQuantity(String sku) {
        return globalQuantityBySku.getOrDefault(sku, 0);
    }

    public Optional<Integer> getStoreQuantity(String storeId, String sku) {
        return Optional.ofNullable(perStoreQuantities.get(storeId + "|" + sku));
    }
}

