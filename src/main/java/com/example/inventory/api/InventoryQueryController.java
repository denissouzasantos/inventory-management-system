package com.example.inventory.api;

import com.example.inventory.service.CentralInventoryProjection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/query/inventory")
public class InventoryQueryController {
    private final CentralInventoryProjection projection;

    public InventoryQueryController(CentralInventoryProjection projection) {
        this.projection = projection;
    }

    @GetMapping("/global/{sku}")
    public ResponseEntity<Map<String, Object>> global(@PathVariable("sku") String sku) {
        int qty = projection.getGlobalQuantity(sku);
        return ResponseEntity.ok(Map.of("sku", sku, "quantity", qty));
    }

    @GetMapping("/store/{storeId}/{sku}")
    public ResponseEntity<Map<String, Object>> perStore(@PathVariable("storeId") String storeId, @PathVariable("sku") String sku) {
        int qty = projection.getStoreQuantity(storeId, sku).orElse(0);
        return ResponseEntity.ok(Map.of("storeId", storeId, "sku", sku, "quantity", qty));
    }
}

