package com.example.inventory;

import com.example.inventory.event.StockAdjusted;
import com.example.inventory.event.StockReplaced;
import com.example.inventory.model.InventoryRecord;
import com.example.inventory.service.InMemoryEventBus;
import com.example.inventory.service.StoreInventoryService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class StoreInventoryServiceTest {

    @Test
    void replace_then_adjust_increments_version_and_quantity() {
        InMemoryEventBus bus = new InMemoryEventBus(ObservationRegistry.create(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        List<Object> events = new ArrayList<>();
        bus.subscribe(e -> events.add(e));
        StoreInventoryService svc = new StoreInventoryService(bus);

        InventoryRecord a = svc.replaceStock("A", "SKU-X", 10);
        InventoryRecord b = svc.adjustStock("A", "SKU-X", 5);

        assertThat(a.getVersion()).isEqualTo(1);
        assertThat(b.getVersion()).isEqualTo(2);
        assertThat(b.getQuantity()).isEqualTo(15);
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(StockReplaced.class);
        assertThat(events.get(1)).isInstanceOf(StockAdjusted.class);
    }

    @Test
    void concurrent_updates_are_atomic() throws Exception {
        InMemoryEventBus bus = new InMemoryEventBus(ObservationRegistry.create(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        StoreInventoryService svc = new StoreInventoryService(bus);

        int threads = 8;
        var pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try { svc.adjustStock("S", "C-SKU", 1); }
                finally { latch.countDown(); }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        InventoryRecord finalRec = svc.getStock("S", "C-SKU").orElseThrow();
        assertThat(finalRec.getQuantity()).isEqualTo(threads);
        assertThat(finalRec.getVersion()).isEqualTo(threads);
    }
}


