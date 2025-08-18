package com.example.inventory.service;

import com.example.inventory.event.InventoryEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Component
public class InMemoryEventBus {
    private final BlockingQueue<InventoryEvent> queue = new LinkedBlockingQueue<>();
    private final List<Consumer<InventoryEvent>> subscribers = new ArrayList<>();

    public void publish(InventoryEvent event) {
        queue.offer(event);
    }

    public void subscribe(Consumer<InventoryEvent> handler) {
        subscribers.add(handler);
    }

    public void start() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    InventoryEvent event = queue.take();
                    for (Consumer<InventoryEvent> s : subscribers) {
                        try {
                            s.accept(event);
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

