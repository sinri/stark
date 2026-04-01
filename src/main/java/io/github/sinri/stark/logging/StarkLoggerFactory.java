package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.format.StarkLogFormatter;
import io.github.sinri.stark.logging.sink.StarkLogSink;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory that creates and caches {@link StarkLogger} instances.
 * <p>
 * Supports runtime reconfiguration: changes to {@link #setFormatter(StarkLogFormatter)}
 * and {@link #setDefaultLevel(Level)} take effect immediately on all existing loggers.
 */
public class StarkLoggerFactory implements ILoggerFactory {

    private final ConcurrentHashMap<String, StarkLogger> loggerMap = new ConcurrentHashMap<>();
    private final StarkMDCAdapter mdcAdapter;
    private volatile StarkLogFormatter formatter;
    private volatile StarkLogSink sink;
    private volatile Level defaultLevel;
    private volatile Map<String, Level> loggerLevels;

    StarkLoggerFactory(StarkLogFormatter formatter, Level defaultLevel, StarkMDCAdapter mdcAdapter) {
        this(formatter, defaultLevel, Map.of(), null, mdcAdapter);
    }

    StarkLoggerFactory(StarkLoggingConfiguration configuration, StarkMDCAdapter mdcAdapter) {
        this(
                configuration.getFormatter(),
                configuration.getDefaultLevel(),
                configuration.getLoggerLevels(),
                configuration.getSink(),
                mdcAdapter
        );
    }

    StarkLoggerFactory(
            StarkLogFormatter formatter,
            Level defaultLevel,
            Map<String, Level> loggerLevels,
            StarkLogSink sink,
            StarkMDCAdapter mdcAdapter
    ) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.defaultLevel = Objects.requireNonNull(defaultLevel, "defaultLevel");
        this.loggerLevels = Map.copyOf(Objects.requireNonNull(loggerLevels, "loggerLevels"));
        this.sink = Objects.requireNonNullElseGet(sink, () -> new io.github.sinri.stark.logging.sink.StdoutLogSink());
        this.mdcAdapter = mdcAdapter;
    }

    public static StarkLoggerFactory installed() {
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if (iLoggerFactory instanceof StarkLoggerFactory starkLoggerFactory) {
            return starkLoggerFactory;
        }
        throw new IllegalStateException("StarkLoggerFactory is not installed as SLF4J provider factory");
    }

    @Override
    public Logger getLogger(String name) {
        Objects.requireNonNull(name, "name");
        return loggerMap.computeIfAbsent(name,
                n -> new StarkLogger(n, this, mdcAdapter));
    }

    public StarkLogFormatter getFormatter() {
        return formatter;
    }

    public StarkLoggerFactory setFormatter(StarkLogFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        return this;
    }

    public StarkLogSink getSink() {
        return sink;
    }

    public StarkLoggerFactory setSink(StarkLogSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
        return this;
    }

    public Level getDefaultLevel() {
        return defaultLevel;
    }

    public StarkLoggerFactory setDefaultLevel(Level defaultLevel) {
        this.defaultLevel = Objects.requireNonNull(defaultLevel, "defaultLevel");
        return this;
    }

    public Map<String, Level> getLoggerLevels() {
        return loggerLevels;
    }

    public synchronized StarkLoggerFactory setLevel(String loggerName, Level level) {
        Objects.requireNonNull(level, "level");
        String normalizedLoggerName = normalizeLoggerName(loggerName);
        LinkedHashMap<String, Level> levels = new LinkedHashMap<>(loggerLevels);
        levels.put(normalizedLoggerName, level);
        loggerLevels = Map.copyOf(levels);
        return this;
    }

    public synchronized StarkLoggerFactory clearLevel(String loggerName) {
        String normalizedLoggerName = normalizeLoggerName(loggerName);
        LinkedHashMap<String, Level> levels = new LinkedHashMap<>(loggerLevels);
        levels.remove(normalizedLoggerName);
        loggerLevels = Map.copyOf(levels);
        return this;
    }

    public synchronized StarkLoggerFactory clearLevelOverrides() {
        loggerLevels = Map.of();
        return this;
    }

    public Level getEffectiveLevel(String loggerName) {
        Objects.requireNonNull(loggerName, "loggerName");
        Level level = loggerLevels.get(loggerName);
        if (level != null) {
            return level;
        }

        int cursor = loggerName.length();
        while ((cursor = loggerName.lastIndexOf('.', cursor - 1)) > 0) {
            String prefix = loggerName.substring(0, cursor);
            level = loggerLevels.get(prefix);
            if (level != null) {
                return level;
            }
        }
        return defaultLevel;
    }

    private String normalizeLoggerName(String loggerName) {
        Objects.requireNonNull(loggerName, "loggerName");
        String normalizedLoggerName = loggerName.trim();
        if (normalizedLoggerName.isEmpty()) {
            throw new IllegalArgumentException("loggerName must not be blank");
        }
        return normalizedLoggerName;
    }
}
