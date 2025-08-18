package com.example.inventory.event;

import java.time.Instant;

public record StockReplaced(String storeId, String sku, int quantity, long version, Instant occurredAt) implements InventoryEvent {}

