package com.example.inventory.service;

import com.example.inventory.event.InventoryEvent;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Component
public class InMemoryEventBus {
    private record Envelope(InventoryEvent event, ContextSnapshot snapshot) {}

    private final BlockingQueue<Envelope> queue = new LinkedBlockingQueue<>();
    private final List<Consumer<InventoryEvent>> subscribers = new ArrayList<>();
    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;

    public InMemoryEventBus(ObservationRegistry observationRegistry, ContextSnapshotFactory snapshotFactory) {
        this.observationRegistry = observationRegistry;
        this.snapshotFactory = snapshotFactory;
    }

    public void publish(InventoryEvent event) {
        Observation.createNotStarted("inventory.event.publish", observationRegistry)
            .lowCardinalityKeyValue("type", event.getClass().getSimpleName())
            .lowCardinalityKeyValue("sku", event.sku())
            .observe(() -> queue.offer(new Envelope(event, snapshotFactory.captureAll())));
    }

    public void subscribe(Consumer<InventoryEvent> handler) {
        subscribers.add(handler);
    }

    public void start() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    Envelope envelope = queue.take();
                    for (Consumer<InventoryEvent> s : subscribers) {
                        try {
                            envelope.snapshot().wrap(() ->
                                Observation.createNotStarted("inventory.event.consume", observationRegistry)
                                    .lowCardinalityKeyValue("type", envelope.event().getClass().getSimpleName())
                                    .lowCardinalityKeyValue("sku", envelope.event().sku())
                                    .observe(() -> s.accept(envelope.event()))
                            );
                        } catch (Exception ignored) {}
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "event-bus-worker");
        worker.setDaemon(true);
        worker.start();
    }
}

