package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;
import io.github.sinri.stark.logging.sink.StarkLogSink;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.DefaultLoggingEventBuilder;
import org.slf4j.spi.LoggingEventAware;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stark's SLF4J Logger implementation.
 * <p>
 * Extends {@link LegacyAbstractLogger} to inherit all 50+ logging method overloads,
 * and implements {@link LoggingEventAware} to support the SLF4J 2.x fluent API.
 * <p>
 * Reads formatter and level from {@link StarkLoggerFactory} at call time,
 * so runtime configuration changes take effect immediately.
 */
public class StarkLogger extends LegacyAbstractLogger implements LoggingEventAware {

    private static final String FQCN = StarkLogger.class.getName();

    private final StarkLoggerFactory factory;
    private final StarkMDCAdapter mdcAdapter;

    StarkLogger(String name, StarkLoggerFactory factory, StarkMDCAdapter mdcAdapter) {
        this.name = name;
        this.factory = factory;
        this.mdcAdapter = mdcAdapter;
    }

    private boolean isLevelEnabled(Level level) {
        return level.toInt() >= factory.getEffectiveLevel(name).toInt();
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(Level.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(Level.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(Level.ERROR);
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return FQCN;
    }

    @Override
    protected void handleNormalizedLoggingCall(
            Level level,
            @Nullable Marker marker,
            String messagePattern,
            @Nullable Object[] arguments,
            @Nullable Throwable throwable
    ) {
        List<Marker> markers = marker != null ? Collections.singletonList(marker) : null;
        output(createEvent(
                level,
                messagePattern,
                arguments,
                throwable,
                System.currentTimeMillis(),
                Thread.currentThread().getName(),
                markers,
                null
        ));
    }

    @Override
    public void log(LoggingEvent loggingEvent) {
        Level level = loggingEvent.getLevel();
        if (!isLevelEnabled(level)) {
            return;
        }

        List<KeyValuePair> kvps = loggingEvent.getKeyValuePairs();
        List<Marker> markers = loggingEvent.getMarkers();

        output(createEvent(
                level,
                loggingEvent.getMessage(),
                loggingEvent.getArgumentArray(),
                loggingEvent.getThrowable(),
                loggingEvent.getTimeStamp(),
                loggingEvent.getThreadName(),
                markers,
                kvps
        ));
    }

    private StarkLoggingEvent createEvent(
            Level level,
            @Nullable String messagePattern,
            @Nullable Object[] arguments,
            @Nullable Throwable throwable,
            long timestamp,
            String threadName,
            @Nullable List<Marker> markers,
            @Nullable List<KeyValuePair> keyValuePairs
    ) {
        String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
        Map<String, String> mdcMap = mdcAdapter.getCopyOfContextMap();
        return new StarkLoggingEvent(
                name,
                level,
                formattedMessage,
                messagePattern,
                arguments,
                throwable,
                timestamp,
                threadName,
                markers,
                mdcMap,
                keyValuePairs
        );
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        if (isLevelEnabled(level)) {
            return new DefaultLoggingEventBuilder(this, level);
        }
        return NOPLoggingEventBuilder.singleton();
    }

    private void output(StarkLoggingEvent event) {
        String line = factory.getFormatter().format(event);
        StarkLogSink sink = factory.getSink();
        sink.write(event, line);
    }
}
