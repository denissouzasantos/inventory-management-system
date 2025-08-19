package com.example.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class Requests {
    public record ReplaceStockRequest(
        @NotBlank String storeId,
        @NotBlank String sku,
        @Min(0) int quantity
    ) {}
    public record AdjustStockRequest(
        @NotBlank String storeId,
        @NotBlank String sku,
        int delta
    ) {}
}

