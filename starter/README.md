# Starter

Your workspace. It has every workshop dependency (Arrow core, fx-coroutines, resilience, Kotest with Arrow
assertions, kotlinx-coroutines-test) and deliberately no source code.

Create `src/main/kotlin/workshop/...` and `src/test/kotlin/workshop/...` as you go, then run from the repository root:

```bash
./gradlew :starter:test
```

Stuck? The numbered directories hold a reference solution for each section. Lint is not applied here, so it
never gets in the way of an exercise; the reference stages run kotlinter and detekt on `./gradlew check`.

The optional extras need two more dependencies; add them when you get there:

```kotlin
implementation(libs.arrow.fx.stm)   // Transactional memory
implementation(libs.arrow.optics)   // Optics, plus alias(libs.plugins.arrow.optics) in plugins { }
```
