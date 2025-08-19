package com.example.inventory.service;

import com.example.inventory.event.InventoryEvent;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class InMemoryEventBus {
    private final List<Consumer<InventoryEvent>> subscribers = new ArrayList<>();
    private final ObservationRegistry observationRegistry;
    private final Counter publishFailures;
    private final Counter consumeFailures;
    private final List<DLQEntry> deadLetterQueue = new CopyOnWriteArrayList<>();

    public InMemoryEventBus(ObservationRegistry observationRegistry, MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.publishFailures = Counter.builder("event_bus.publish.failures").register(meterRegistry);
        this.consumeFailures = Counter.builder("event_bus.consume.failures").register(meterRegistry);
    }

    public record DLQEntry(InventoryEvent event, String errorMessage, int attempts) {}

    // Consistency-first: deliver to all subscribers synchronously with bounded retries
    public void publish(InventoryEvent event) {
        Observation.createNotStarted("inventory.event.publish", observationRegistry)
            .lowCardinalityKeyValue("type", event.getClass().getSimpleName())
            .lowCardinalityKeyValue("sku", event.sku())
            .observe(() -> {
                for (Consumer<InventoryEvent> s : subscribers) {
                    boolean delivered = false;
                    int attempts = 0;
                    Exception last = null;
                    while (!delivered && attempts < 3) {
                        attempts++;
                        try {
                            Observation.createNotStarted("inventory.event.consume", observationRegistry)
                                .lowCardinalityKeyValue("type", event.getClass().getSimpleName())
                                .lowCardinalityKeyValue("sku", event.sku())
                                .observe(() -> s.accept(event));
                            delivered = true;
                        } catch (Exception e) {
                            last = e;
                            consumeFailures.increment();
                            try { Thread.sleep(10L * (1L << (attempts - 1))); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        }
                    }
                    if (!delivered) {
                        publishFailures.increment();
                        deadLetterQueue.add(new DLQEntry(event, last == null ? "unknown" : last.getMessage(), attempts));
                        throw new RuntimeException("Failed to deliver event to all subscribers; moved to DLQ after " + attempts + " attempts", last);
                    }
                }
            });
    }

    public void subscribe(Consumer<InventoryEvent> handler) {
        subscribers.add(handler);
    }

    // Kept for backward compatibility; no-op in synchronous mode
    public void start() { }

    public List<DLQEntry> deadLetters() {
        return deadLetterQueue;
    }
}

