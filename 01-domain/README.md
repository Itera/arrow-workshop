# 1. Domain and immutable data

Plain immutable data classes, small value types (`Money`, `Courier`), and nested updates with `copy`.

- `domain/Domain.kt`: the model and the two `copy`-based updates. Note how much code `withParcelWeight` needs;
  `09-extras` revisits it with optics.
- `test/.../Fixtures.kt`: request builders reused by every later stage.

```bash
./gradlew :01-domain:test
```
