# 5. Parallel courier quotes

Builds on stages 1–4 and completes the vertical slice: validate, apply rules, fetch quotes concurrently, and return
the cheapest eligible one.

- `quoting/CourierProvider.kt`: the provider interface and `CourierDecline` (`NoService` vs `Prohibited`).
- `quoting/ParallelQuotes.kt`: bounded `parMapNotNull`, a `parZip` variant, the skip-or-abort policy, and quote selection.
- `test/.../FakeCourier.kt`: a configurable fake that records calls and cancellation.

The tests use `runTest` virtual time to prove parallelism, the concurrency limit, and sibling cancellation.

```bash
./gradlew :05-parallel-quotes:test
```
