# inventory-management-system
inventory-management-system
## Distributed Inventory Management (Prototype)

This repository contains a simplified, production-inspired prototype for optimizing a distributed inventory management system using Java 21 and Spring Boot 3. It demonstrates event-driven consistency, low-latency writes, and a fast read model suitable for a chain of retail stores.

### Architecture

- **Store Command Service**: Accepts stock updates from stores and emits domain events (`StockReplaced`, `StockAdjusted`).
- **In-Memory Event Bus**: A lightweight async queue simulating Kafka/Pulsar. Events are delivered to subscribers in the background thread.
- **Central Query Projection**: Subscribes to events and maintains denormalized views for quick queries:
  - Total quantity by SKU across all stores
  - Per-store quantity by SKU

This follows CQRS + Event-Driven principles. Commands are accepted immediately (availability). The central projection converges to the latest state with basic last-write-wins using per-partition versions.

### API

Commands

POST `/api/commands/inventory/replace`
```json
{ "storeId": "S1", "sku": "ABC-123", "quantity": 42 }
```

POST `/api/commands/inventory/adjust`
```json
{ "storeId": "S1", "sku": "ABC-123", "delta": -3 }
```

Queries

GET `/api/query/inventory/global/{sku}` -> `{ sku, quantity }`

GET `/api/query/inventory/store/{storeId}/{sku}` -> `{ storeId, sku, quantity }`

### Run locally

Prerequisites: Java 21, Maven 3.9+

```bash
mvn spring-boot:run
```

Open another terminal to try the flow:

```bash
curl -s -X POST localhost:8080/api/commands/inventory/replace \
  -H 'Content-Type: application/json' \
  -d '{"storeId":"A","sku":"SKU1","quantity":10}'

curl -s -X POST localhost:8080/api/commands/inventory/adjust \
  -H 'Content-Type: application/json' \
  -d '{"storeId":"B","sku":"SKU1","delta":5}'

sleep 0.2

curl -s localhost:8080/api/query/inventory/global/SKU1 | jq
```

### Observability

- Actuator endpoints enabled; Prometheus metrics available at `/actuator/prometheus`.

### Fault tolerance (prototype)

- Asynchronous event delivery isolates command latency from projection work.
- Idempotency and staleness handling via per `storeId|sku` version numbers; stale events are ignored.
- Last-write-wins per partition for simplicity; can be upgraded to vector clocks or CRDTs.

### Trade-offs and extensions

- In-memory bus and state are ephemeral; replace with Kafka and a persistent store for durability.
- Per-store snapshots are kept in memory to compute global totals. In production, use a compacted topic or a key-value store (Redis/RocksDB) for this map.
- Add inventory reservations, sagas for order flows, and reconciliation jobs.

### Tests

Run the integration test:

```bash
mvn -q -DskipTests=false test
```

### Tech stack

- Java 21, Spring Boot 3.3
- Web + Actuator + Micrometer
