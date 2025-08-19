package com.example.inventory.api;

import com.example.inventory.api.dto.Requests.AdjustStockRequest;
import com.example.inventory.api.dto.Requests.ReplaceStockRequest;
import com.example.inventory.model.InventoryRecord;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.example.inventory.service.StoreInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.observation.annotation.Observed;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/commands/inventory")
public class InventoryCommandController {
    private final StoreInventoryService storeInventoryService;

    public InventoryCommandController(StoreInventoryService storeInventoryService) {
        this.storeInventoryService = storeInventoryService;
    }

    @PostMapping("/replace")
    @Observed(name = "inventory.command.replace")
    public ResponseEntity<InventoryRecord> replace(@RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                   @Valid @RequestBody ReplaceStockRequest request) {
        long expectedVersion = parseIfMatch(ifMatch);
        InventoryRecord record;
        try {
            record = storeInventoryService.replaceStockWithOptimisticLock(request.storeId(), request.sku(), request.quantity(), expectedVersion);
        } catch (com.example.inventory.service.StoreInventoryService.OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "ETag mismatch. Current version=" + e.getCurrentVersion());
        }
        return withEtag(record);
    }

    @PostMapping("/adjust")
    @Observed(name = "inventory.command.adjust")
    public ResponseEntity<InventoryRecord> adjust(@RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                  @Valid @RequestBody AdjustStockRequest request) {
        long expectedVersion = parseIfMatch(ifMatch);
        InventoryRecord record;
        try {
            record = storeInventoryService.adjustStockWithOptimisticLock(request.storeId(), request.sku(), request.delta(), expectedVersion);
        } catch (com.example.inventory.service.StoreInventoryService.OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "ETag mismatch. Current version=" + e.getCurrentVersion());
        }
        return withEtag(record);
    }

    private long parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) return -1L;
        try {
            return Long.parseLong(ifMatch.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            return -2L;
        }
    }

    private ResponseEntity<InventoryRecord> withEtag(InventoryRecord record) {
        return ResponseEntity.status(HttpStatus.OK)
            .eTag('"' + String.valueOf(record.getVersion()) + '"')
            .body(record);
    }
}

