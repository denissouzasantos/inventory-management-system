package com.example.inventory.model;

import java.time.Instant;

public class InventoryRecord {
    private String storeId;
    private String sku;
    private int quantity;
    private long version;
    private Instant updatedAt;

    public InventoryRecord() {}

    public InventoryRecord(String storeId, String sku, int quantity, long version, Instant updatedAt) {
        this.storeId = storeId;
        this.sku = sku;
        this.quantity = quantity;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

