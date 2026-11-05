# 2. Validation: report everything wrong

Builds on stage 1. `validate` returns `Either<NonEmptyList<ShipmentValidationError>, Shipment>` and reports every
independent problem at once, using the stable accumulation builders `zipOrAccumulate`, `mapOrAccumulate` and `withNel`.

- `validation/Validation.kt`: the validation and the `Shipment` type it produces. `Shipment` has an internal
  constructor, so a `Shipment` is proof of validation.

```bash
./gradlew :02-validation:test
```
