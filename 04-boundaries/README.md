# 4. Absence and existing Kotlin APIs

Builds on stages 1–3. Nullable types for local absence, and an adapter that classifies failures from an
exception-oriented `Result` API.

- `providers/ZoneLookup.kt`: the legacy API we do not own.
- `quoting/Zones.kt`: the adapter. Unknown postcode becomes the domain error `QuoteError.UnknownPostcode`;
  any other failure stays an exception.
- `quoting/Surcharges.kt`: insurance and pickup fees applied only when present.

```bash
./gradlew :04-boundaries:test
```
