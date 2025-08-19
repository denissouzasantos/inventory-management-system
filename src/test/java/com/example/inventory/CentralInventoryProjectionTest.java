package com.example.inventory;

import com.example.inventory.event.StockAdjusted;
import com.example.inventory.event.StockReplaced;
import com.example.inventory.service.CentralInventoryProjection;
import com.example.inventory.service.InMemoryEventBus;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CentralInventoryProjectionTest {

    @Test
    void ignores_stale_events_by_version() {
        InMemoryEventBus bus = new InMemoryEventBus(ObservationRegistry.create(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        CentralInventoryProjection proj = new CentralInventoryProjection(bus);
        proj.subscribe();

        // Newer version first
        bus.publish(new StockReplaced("S1", "SKU-V", 10, 2, java.time.Instant.now()));
        // Stale version should be ignored
        bus.publish(new StockReplaced("S1", "SKU-V", 5, 1, java.time.Instant.now()));

        assertThat(proj.getGlobalQuantity("SKU-V")).isEqualTo(10);
    }

    @Test
    void aggregates_across_stores() {
        InMemoryEventBus bus = new InMemoryEventBus(ObservationRegistry.create(), new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        CentralInventoryProjection proj = new CentralInventoryProjection(bus);
        proj.subscribe();

        bus.publish(new StockReplaced("A", "SKU-Z", 4, 1, java.time.Instant.now()));
        bus.publish(new StockReplaced("B", "SKU-Z", 6, 1, java.time.Instant.now()));
        bus.publish(new StockAdjusted("A", "SKU-Z", 1, 2, java.time.Instant.now()));

        assertThat(proj.getGlobalQuantity("SKU-Z")).isEqualTo(11);
        assertThat(proj.getStoreQuantity("A", "SKU-Z").orElse(-1)).isEqualTo(5);
        assertThat(proj.getStoreQuantity("B", "SKU-Z").orElse(-1)).isEqualTo(6);
    }
}


