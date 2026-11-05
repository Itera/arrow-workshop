# 6. Live quote updates

Builds on stages 1–5. Each provider outcome is emitted as it completes. Failures are data in the stream
(`QuoteUpdate.Failed`), not a `Raise` scope kept open by a long-lived flow.

- `quoting/QuoteUpdate.kt`: `liveQuoteUpdates` (`parMapUnordered`) and `orderedQuoteUpdates` (`parMap`), using
  `Either.catch`, which rethrows cancellation, rather than `runCatching`, which swallows it.

```bash
./gradlew :06-live-updates:test
```
