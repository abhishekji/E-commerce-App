# Architecture and production plan

## Boundaries

Each service owns an aggregate and persistence model: catalog/product, inventory/stock item, order/checkout, payment, and notification preference/delivery. No service reads another service's database. Synchronous calls are limited to latency-sensitive reads; business workflows use Kafka.

## Checkout saga

1. Order service validates the cart and persists `PENDING_PAYMENT`.
2. It publishes `commerce.order.events.v1: OrderPlaced`.
3. Inventory reserves stock; payment authorizes funds.
4. Order transitions to `CONFIRMED` only after both outcomes.
5. Any failure emits a compensating command (`ReleaseInventory` or `RefundPayment`) and transitions to `CANCELLED`.

The production implementation must use a transactional outbox and an inbox/deduplication table. Kafka consumers use manual acknowledgement, exponential backoff, and a dead-letter topic.

## Implemented platform primitives

- Catalog product reads use a ten-minute Redis read-through cache.
- Catalog mutations explicitly evict `catalog:product:{id}`; TTL remains the safety net for missed invalidation and stale entries.
- Order creation uses a 24-hour Redis `SET NX` idempotency key.
- Kafka producers publish JSON with an explicit topic version; consumers use independent consumer groups.
- `@UseCase` plus an AspectJ interceptor records timer metrics for application use cases.
- Correlation IDs are propagated as `X-Correlation-Id`; Actuator exposes health and Prometheus endpoints.

The current payment and inventory adapters are deterministic local implementations suitable for integration demos. Replace them with database-backed adapters and provider clients behind ports before production.

## How events are used

Kafka events are facts, not remote procedure calls. `OrderPlaced` is consumed independently by inventory (reserve stock), payment (authorize funds), and notification (inform the customer). Their outcome events are consumed by order (state transition) and notification (customer update). This fan-out avoids synchronous coupling and allows each consumer to scale and retry independently.

## Cache policy

Catalog uses cache-aside: reads check Redis, load the service-owned source, then set a ten-minute TTL. Product updates write the source first and explicitly delete the product key. The TTL bounds staleness if an eviction fails; production should add versioned keys, metrics for hit/miss/eviction, and publish invalidation events when catalog data is replicated.

## Non-functional targets

- 99.95% monthly availability for browse and checkout APIs.
- p95 browse latency below 200 ms; p95 checkout acceptance below 500 ms.
- RPO 0 for committed orders; RTO below 30 minutes.
- Per-tenant rate limits and bounded request payloads at the gateway.

## Delivery roadmap

1. Add Flyway schemas, repositories, outbox/inbox, and Testcontainers integration tests.
2. Add OIDC resource-server security, authorization policies, and PCI-isolated payment adapter.
3. Add OpenTelemetry, Grafana dashboards, alert rules, and structured audit logs.
4. Package services as OCI images and deploy with Helm/Kubernetes using canary releases.
