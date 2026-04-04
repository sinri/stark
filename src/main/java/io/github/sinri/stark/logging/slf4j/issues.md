# Issues

## 1. `StarkSlf4jServiceProvider` — `@NullMarked` violation

**Priority: High**

Fields `loggerFactory`, `markerFactory`, and `mdcAdapter` are implicitly non-null under `@NullMarked`,
but are Java-default `null` until `initialize()` is called.

Fix: annotate all three fields with `@Nullable`.

```java
private @Nullable ILoggerFactory loggerFactory;
private @Nullable IMarkerFactory markerFactory;
private @Nullable MDCAdapter mdcAdapter;
```

## 2. `StarkSlf4jLogger` — extends deprecated `MarkerIgnoringBase`

**Priority: High**

`MarkerIgnoringBase` is deprecated since SLF4J 2.0. The correct base class is
`org.slf4j.helpers.AbstractLogger`, which requires implementing one method instead of 25 overrides:

```java
protected void handleNormalizedLoggingCall(
    Level level, Marker marker, String msg, Object[] args, Throwable throwable)
```

Fix: replace `extends MarkerIgnoringBase` with `extends AbstractLogger` and collapse all
per-level overrides into a single `handleNormalizedLoggingCall` implementation.
Also override `isEnabledForLevel(Level)` to replace the five `isXxxEnabled()` methods.

## 3. `StarkSlf4jLogger` — missing `serialVersionUID`

**Priority: Low**

The class extends a `Serializable` chain (`MarkerIgnoringBase` / `AbstractLogger`) without
declaring `serialVersionUID`, which produces a compiler warning and risks accidental
serialization incompatibility.

Fix: add `private static final long serialVersionUID = 1L;`.

## 4. `README.md` — written in Chinese

**Priority: Low**

Project convention (CLAUDE.md) requires all documentation to be in English.
The README in this package is written in Chinese.

Fix: rewrite `README.md` in English.
