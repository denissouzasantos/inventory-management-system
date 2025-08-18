package com.example.inventory.event;

import java.time.Instant;

public sealed interface InventoryEvent permits StockAdjusted, StockReplaced {
    String storeId();
    String sku();
    long version();
    Instant occurredAt();
}

