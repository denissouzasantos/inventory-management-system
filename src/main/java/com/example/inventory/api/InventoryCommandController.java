package com.example.inventory.api;

import com.example.inventory.api.dto.Requests.AdjustStockRequest;
import com.example.inventory.api.dto.Requests.ReplaceStockRequest;
import com.example.inventory.model.InventoryRecord;
import com.example.inventory.service.StoreInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commands/inventory")
public class InventoryCommandController {
    private final StoreInventoryService storeInventoryService;

    public InventoryCommandController(StoreInventoryService storeInventoryService) {
        this.storeInventoryService = storeInventoryService;
    }

    @PostMapping("/replace")
    public ResponseEntity<InventoryRecord> replace(@RequestBody ReplaceStockRequest request) {
        InventoryRecord record = storeInventoryService.replaceStock(request.storeId(), request.sku(), request.quantity());
        return ResponseEntity.ok(record);
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryRecord> adjust(@RequestBody AdjustStockRequest request) {
        InventoryRecord record = storeInventoryService.adjustStock(request.storeId(), request.sku(), request.delta());
        return ResponseEntity.ok(record);
    }
}

