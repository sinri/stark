package io.github.sinri.stark.logging.event;

import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;

/**
 * Immutable data carrier for a single logging event.
 * <p>
 * Captures all contextual information produced by an SLF4J logging call,
 * including the formatted message, level, thread, MDC context, markers,
 * and structured key-value pairs from the fluent API.
 * Instances are created by {@link io.github.sinri.stark.logging.StarkLogger}
 * and passed to a {@link io.github.sinri.stark.logging.format.StarkLogFormatter}
 * for rendering.
 */
public class StarkLoggingEvent {
    private final String loggerName;
    private final Level level;
    private final String message;
    private final @Nullable String messagePattern;
    private final @Nullable Object[] arguments;
    private final @Nullable Throwable throwable;
    private final long timestamp;
    private final String threadName;
    private final @Nullable List<Marker> markers;
    private final @Nullable Map<String, String> mdcMap;
    private final @Nullable List<KeyValuePair> keyValuePairs;

    /**
     * @param loggerName    the name of the logger that produced this event
     * @param level         the log level
     * @param message       the fully formatted log message (placeholders already resolved)
     * @param messagePattern the original message template before placeholder expansion
     * @param arguments     the raw message arguments used during formatting
     * @param throwable     an optional throwable associated with the event
     * @param timestamp     epoch milliseconds when the event was created
     * @param threadName    the name of the thread that produced the event
     * @param markers       optional SLF4J markers attached to this event
     * @param mdcMap        a snapshot of the MDC context at the time of the call
     * @param keyValuePairs structured key-value pairs from the SLF4J fluent API
     */
    public StarkLoggingEvent(
            String loggerName,
            Level level,
            String message,
            @Nullable String messagePattern,
            @Nullable Object[] arguments,
            @Nullable Throwable throwable,
            long timestamp,
            String threadName,
            @Nullable List<Marker> markers,
            @Nullable Map<String, String> mdcMap,
            @Nullable List<KeyValuePair> keyValuePairs
    ) {
        this.loggerName = loggerName;
        this.level = level;
        this.message = message;
        this.messagePattern = messagePattern;
        this.arguments = arguments != null ? arguments.clone() : null;
        this.throwable = throwable;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.markers = markers != null ? List.copyOf(markers) : null;
        this.mdcMap = mdcMap != null ? Map.copyOf(mdcMap) : null;
        this.keyValuePairs = keyValuePairs != null ? List.copyOf(keyValuePairs) : null;
    }

    /** Returns the name of the logger that produced this event. */
    public String getLoggerName() {
        return loggerName;
    }

    /** Returns the log level (TRACE, DEBUG, INFO, WARN, or ERROR). */
    public Level getLevel() {
        return level;
    }

    /** Returns the fully formatted log message (all {@code {}} placeholders resolved). */
    public String getMessage() {
        return message;
    }

    /** Returns the original message template, or {@code null} if none was provided. */
    public @Nullable String getMessagePattern() {
        return messagePattern;
    }

    /** Returns a copy of the raw message arguments, or {@code null} if none were provided. */
    public @Nullable Object[] getArguments() {
        return arguments != null ? arguments.clone() : null;
    }

    /** Returns the throwable associated with this event, or {@code null} if none. */
    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    /** Returns the event creation time as epoch milliseconds. */
    public long getTimestamp() {
        return timestamp;
    }

    /** Returns the name of the thread that produced this event. */
    public String getThreadName() {
        return threadName;
    }

    /** Returns the SLF4J markers attached to this event, or {@code null} if none. */
    public @Nullable List<Marker> getMarkers() {
        return markers != null ? List.copyOf(markers) : null;
    }

    /** Returns a snapshot of the MDC context map, or {@code null} if empty. */
    public @Nullable Map<String, String> getMdcMap() {
        return mdcMap != null ? Map.copyOf(mdcMap) : null;
    }

    /** Returns the structured key-value pairs from the SLF4J fluent API, or {@code null} if none. */
    public @Nullable List<KeyValuePair> getKeyValuePairs() {
        return keyValuePairs != null ? List.copyOf(keyValuePairs) : null;
    }
}
