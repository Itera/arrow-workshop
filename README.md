# Arrow workshop: resilient delivery quotes

A self-guided workshop that builds one small Delivery Quote Service with [Arrow](https://arrow-kt.io).
The full handout is [`arrow-workshop.md`](arrow-workshop.md) (also as [PDF](arrow-workshop.pdf)).

## Requirements

- Java 25 (Eclipse Temurin recommended). If it is missing, the Foojay toolchain resolver downloads one.
- Nothing else: use the Gradle wrapper (`./gradlew`), which pins Gradle 9.8.2.

Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml): Kotlin 2.4.21, Arrow 2.3.0,
Kotest 6.2.6, kotlinx-coroutines 1.11.0.

## Layout

| Directory | Contents |
|---|---|
| `starter/` | Empty workspace with all dependencies. Do the exercises here. |
| `01-domain/` … `08-resource-safety/` | Reference solution after each section. Each is a complete, standalone copy, so you can diff neighbouring stages to see exactly what a section adds. |
| `09-extras/` | Reference solutions for the optional extras: optics, racing, STM, sagas. |

## Commands

```bash
./gradlew :starter:test                 # your work
./gradlew :05-parallel-quotes:test      # one reference stage
./gradlew test                          # every stage
./gradlew check                         # tests + kotlinter + detekt (reference stages only)
./gradlew :05-parallel-quotes:formatKotlin
diff -r 04-boundaries/src 05-parallel-quotes/src   # what section 5 adds
```

To rebuild the PDF: `pandoc arrow-workshop.md -o arrow-workshop.pdf --pdf-engine=pdflatex`.
