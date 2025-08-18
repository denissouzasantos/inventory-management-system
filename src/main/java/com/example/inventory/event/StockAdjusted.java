package com.example.inventory.event;

import java.time.Instant;

public record StockAdjusted(String storeId, String sku, int delta, long version, Instant occurredAt) implements InventoryEvent {}

