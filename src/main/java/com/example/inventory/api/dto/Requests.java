package com.example.inventory.api.dto;

public class Requests {
    public record ReplaceStockRequest(String storeId, String sku, int quantity) {}
    public record AdjustStockRequest(String storeId, String sku, int delta) {}
}

