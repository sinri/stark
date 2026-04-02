# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

Maven project targeting **JDK 25+**. No Maven wrapper is included — use system `mvn`.

```bash
mvn compile        # compile
mvn test           # run all tests (JUnit 5)
mvn test -Dtest=LogTest                    # run a single test class
mvn test -Dtest=LogTest#constructorSetsDefaults  # run a single test method
mvn package        # build jar
```

## Architecture

Stark is a lightweight Java framework built on **Vert.x 5.0.10**, successor to Framework Keel, designed to be AI-coding-friendly.

Root package: `io.github.sinri.stark`

| Package | Purpose |
|---|---|
| `core` | Foundation types. `DataEntity` extends Vert.x `JsonObject` as base data representation. |
| `logging` | Custom logging subsystem: `Log` (event) → `LogRender<R>` (format) → `LogProcesser` (output). `Logger` is the topic-based entry point. |
| `database.mysql` | `StarkMySQLPool` wraps Vert.x reactive MySQL client with static factory methods. |
| `program` | `ProgramContext` — abstract application context holding named MySQL pools. |

### Key Patterns

- **Interface + Impl separation**: public interfaces in parent package, implementations in `impl` sub-packages
- **Singleton via `static final INSTANCE`** with private constructors (e.g., `PlainLogProcesser.INSTANCE`)
- **Static factory methods** on interfaces (e.g., `StarkMySQLPool.pool(...)`)
- **Delegation/wrapper**: implementations wrap Vert.x objects and delegate calls
- **Fluent setters** returning `this` on data entities
- **Framework classes use `Stark` prefix** (e.g., `StarkMySQLPool`, `StarkMySQLPoolImpl`)

## Conventions

- **Nullability**: every package **must** have a `package-info.java` with `@NullMarked` (from `org.jspecify.annotations`). Use `@Nullable` for nullable parameters/returns.
- **No Lombok**: write getters and setters explicitly.
- **No ORM**: use direct SQL via Vert.x MySQL client; represent data as `DataEntity` (extends `JsonObject`).
- **Web endpoints**: one class per API endpoint as the final handler.
- **Tests**: JUnit 5 under `src/test/java/` mirroring main source structure. Class name = `<ClassUnderTest>Test`.
- **Javadoc**: all Javadoc must be written in English.
