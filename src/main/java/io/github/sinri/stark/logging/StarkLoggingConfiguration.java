package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.format.JsonLogFormatter;
import io.github.sinri.stark.logging.format.StarkLogFormatter;
import io.github.sinri.stark.logging.format.TextLogFormatter;
import io.github.sinri.stark.logging.sink.StarkLogSink;
import io.github.sinri.stark.logging.sink.StdoutLogSink;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable runtime configuration for Stark Logging.
 */
final class StarkLoggingConfiguration {
    static final String FORMAT_PROPERTY = "stark.logging.format";
    static final String FORMAT_ENV = "STARK_LOGGING_FORMAT";
    static final String FORMATTER_CLASS_PROPERTY = "stark.logging.formatter.class";
    static final String FORMATTER_CLASS_ENV = "STARK_LOGGING_FORMATTER_CLASS";
    static final String LEVEL_PROPERTY = "stark.logging.level";
    static final String LEVEL_ENV = "STARK_LOGGING_LEVEL";
    static final String LEVELS_PROPERTY = "stark.logging.levels";
    static final String LEVELS_ENV = "STARK_LOGGING_LEVELS";

    private final StarkLogFormatter formatter;
    private final StarkLogSink sink;
    private final Level defaultLevel;
    private final Map<String, Level> loggerLevels;
    private final List<String> diagnostics;

    private StarkLoggingConfiguration(
            StarkLogFormatter formatter,
            StarkLogSink sink,
            Level defaultLevel,
            Map<String, Level> loggerLevels,
            List<String> diagnostics
    ) {
        this.formatter = formatter;
        this.sink = sink;
        this.defaultLevel = defaultLevel;
        this.loggerLevels = loggerLevels;
        this.diagnostics = diagnostics;
    }

    static StarkLoggingConfiguration load() {
        return fromLookup(StarkLoggingConfiguration::getConfig);
    }

    static StarkLoggingConfiguration fromMaps(Map<String, String> systemProperties, Map<String, String> environmentVariables) {
        return fromLookup((sysProp, envVar) -> {
            String value = systemProperties.get(sysProp);
            if (value != null) {
                return value;
            }
            return environmentVariables.get(envVar);
        });
    }

    StarkLogFormatter getFormatter() {
        return formatter;
    }

    StarkLogSink getSink() {
        return sink;
    }

    Level getDefaultLevel() {
        return defaultLevel;
    }

    Map<String, Level> getLoggerLevels() {
        return loggerLevels;
    }

    List<String> getDiagnostics() {
        return diagnostics;
    }

    private static StarkLoggingConfiguration fromLookup(ConfigLookup lookup) {
        ArrayList<String> diagnostics = new ArrayList<>();
        StarkLogFormatter formatter = resolveFormatter(lookup, diagnostics);
        LevelAndOverrides levelAndOverrides = resolveLevels(lookup, diagnostics);

        return new StarkLoggingConfiguration(
                formatter,
                new StdoutLogSink(),
                levelAndOverrides.defaultLevel(),
                Map.copyOf(levelAndOverrides.loggerLevels()),
                List.copyOf(diagnostics)
        );
    }

    private static StarkLogFormatter resolveFormatter(ConfigLookup lookup, List<String> diagnostics) {
        String customClass = lookup.get(FORMATTER_CLASS_PROPERTY, FORMATTER_CLASS_ENV);
        if (customClass != null) {
            try {
                return Class.forName(customClass)
                            .asSubclass(StarkLogFormatter.class)
                            .getDeclaredConstructor()
                            .newInstance();
            } catch (ReflectiveOperationException | ClassCastException e) {
                diagnostics.add("Stark Logging: failed to load custom formatter '" + customClass + "', falling back to configured built-in formatter. " + e);
            }
        }

        String format = lookup.get(FORMAT_PROPERTY, FORMAT_ENV);
        if (format == null || format.isBlank() || "text".equalsIgnoreCase(format)) {
            return new TextLogFormatter();
        }
        if ("json".equalsIgnoreCase(format)) {
            return new JsonLogFormatter();
        }
        diagnostics.add("Stark Logging: invalid format '" + format + "', defaulting to text.");
        return new TextLogFormatter();
    }

    private static LevelAndOverrides resolveLevels(ConfigLookup lookup, List<String> diagnostics) {
        Level defaultLevel = parseLevel(
                lookup.get(LEVEL_PROPERTY, LEVEL_ENV),
                Level.INFO,
                "default",
                diagnostics
        );

        LinkedHashMap<String, Level> loggerLevels = new LinkedHashMap<>();
        String rawLevels = lookup.get(LEVELS_PROPERTY, LEVELS_ENV);
        if (rawLevels == null || rawLevels.isBlank()) {
            return new LevelAndOverrides(defaultLevel, loggerLevels);
        }

        for (String segment : rawLevels.split("[,;\\n]+")) {
            String entry = segment.trim();
            if (entry.isEmpty()) {
                continue;
            }

            int delimiterIndex = entry.indexOf('=');
            if (delimiterIndex <= 0 || delimiterIndex == entry.length() - 1) {
                diagnostics.add("Stark Logging: invalid logger level override '" + entry + "', expected logger=LEVEL.");
                continue;
            }

            String loggerName = entry.substring(0, delimiterIndex).trim();
            String levelName = entry.substring(delimiterIndex + 1).trim();
            Level parsedLevel = parseLevel(levelName, null, "override '" + loggerName + "'", diagnostics);
            if (parsedLevel == null) {
                continue;
            }

            if ("root".equalsIgnoreCase(loggerName) || "*".equals(loggerName)) {
                defaultLevel = parsedLevel;
            } else {
                loggerLevels.put(loggerName, parsedLevel);
            }
        }

        return new LevelAndOverrides(defaultLevel, loggerLevels);
    }

    private static @Nullable Level parseLevel(
            @Nullable String rawLevel,
            @Nullable Level fallbackLevel,
            String label,
            List<String> diagnostics
    ) {
        if (rawLevel == null || rawLevel.isBlank()) {
            return fallbackLevel;
        }
        try {
            return Level.valueOf(rawLevel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            if (fallbackLevel == null) {
                diagnostics.add("Stark Logging: invalid level '" + rawLevel + "' for " + label + "; ignoring override.");
            } else {
                diagnostics.add("Stark Logging: invalid level '" + rawLevel + "', defaulting to " + fallbackLevel + ".");
            }
            return fallbackLevel;
        }
    }

    private static @Nullable String getConfig(String sysProp, String envVar) {
        String value = System.getProperty(sysProp);
        if (value != null) {
            return value;
        }
        return System.getenv(envVar);
    }

    private interface ConfigLookup {
        @Nullable String get(String sysProp, String envVar);
    }

    private record LevelAndOverrides(Level defaultLevel, Map<String, Level> loggerLevels) {
    }
}
