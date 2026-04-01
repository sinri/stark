package io.github.sinri.stark.logging.format;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;
import org.slf4j.event.KeyValuePair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Formats log events as human-readable text lines.
 * <p>
 * Example output:
 * <pre>
 * 2026-04-01 14:30:00.123 [main] INFO  io.github.sinri.App - Hello World
 *   markers: audit security
 *   mdc: requestId=req-1 traceId=abc
 *   kvp: key1=value1 key2=value2
 *   java.lang.RuntimeException: something went wrong
 *     at ...
 * </pre>
 */
public class TextLogFormatter implements StarkLogFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    @Override
    public String format(StarkLoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);

        // timestamp
        sb.append(TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(event.getTimestamp())));

        // thread
        sb.append(" [").append(event.getThreadName()).append(']');

        // level (right-padded to 5 chars)
        String levelName = event.getLevel().name();
        sb.append(' ').append(levelName);
        for (int i = levelName.length(); i < 5; i++) {
            sb.append(' ');
        }

        // logger name
        sb.append(' ').append(event.getLoggerName());

        // message
        sb.append(" - ").append(event.getMessage());

        // markers
        List<String> markers = LogEventFormattingSupport.sortedMarkerNames(event.getMarkers());
        if (!markers.isEmpty()) {
            sb.append('\n')
              .append("  markers: ")
              .append(String.join(" ", markers));
        }

        // MDC
        Map<String, String> mdc = LogEventFormattingSupport.sortedMdc(event.getMdcMap());
        if (!mdc.isEmpty()) {
            sb.append('\n')
              .append("  mdc: ")
              .append(LogEventFormattingSupport.renderEntries(mdc));
        }

        // key-value pairs
        List<KeyValuePair> kvps = LogEventFormattingSupport.sortedKeyValuePairs(event.getKeyValuePairs());
        if (!kvps.isEmpty()) {
            sb.append('\n')
              .append("  kvp: ")
              .append(LogEventFormattingSupport.renderKeyValuePairs(kvps));
        }

        // throwable
        if (event.getThrowable() != null) {
            sb.append('\n').append("  ");
            StringWriter sw = new StringWriter();
            event.getThrowable().printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }

        return sb.toString();
    }
}
