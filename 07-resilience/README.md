# 7. Resilient provider integrations

Builds on stages 1–6. Retries with exponential backoff for transient failures only, a circuit breaker that counts
exceptions but not declines, and classification of leftover technical failures into `CourierDecline.Unavailable`
so one broken courier cannot fail a whole quote.

- `resilience/ResilientProvider.kt`: `RetryingProvider`, `CircuitBreakingProvider`, `FailSoftProvider`, and `resilient()`, which composes them.

```bash
./gradlew :07-resilience:test
```
