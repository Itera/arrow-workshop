# 3. Typed domain errors

Builds on stages 1–2. A public `Either` boundary around direct-style business code using
`context(_: Raise<QuoteError>)`.

- `quoting/QuoteError.kt`: the sealed error hierarchy, including `InvalidRequest` wrapping the validation errors.
- `quoting/QuoteRules.kt`: route, total-weight, and service rules, which fail fast.
- `quoting/QuoteService.kt`: `either { }` at the boundary. Without couriers it ends in `NoQuotesAvailable`; section 5 completes it.

```bash
./gradlew :03-typed-errors:test
```
