package io.github.sinri.stark.logging.format;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.event.KeyValuePair;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats log events as single-line JSON using Vert.x {@link JsonObject}.
 * <p>
 * Example output:
 * <pre>
 * {"timestamp":"2026-04-01T14:30:00.123Z","level":"INFO","logger":"io.github.sinri.App","thread":"main","message":"Hello World","markers":["audit"],"mdc":{"traceId":"abc"},"kvp":{"k":"v"},"exception":"..."}
 * </pre>
 */
public class JsonLogFormatter implements StarkLogFormatter {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    @Override
    public String format(StarkLoggingEvent event) {
        JsonObject json = new JsonObject();

        json.put("timestamp", ISO_FORMATTER.format(Instant.ofEpochMilli(event.getTimestamp())));
        json.put("level", event.getLevel().name());
        json.put("logger", event.getLoggerName());
        json.put("thread", event.getThreadName());
        json.put("message", event.getMessage());

        // Markers
        List<String> markers = LogEventFormattingSupport.sortedMarkerNames(event.getMarkers());
        if (!markers.isEmpty()) {
            json.put("markers", new JsonArray(markers));
        }

        // MDC
        Map<String, String> mdc = LogEventFormattingSupport.sortedMdc(event.getMdcMap());
        if (!mdc.isEmpty()) {
            LinkedHashMap<String, Object> mdcJson = new LinkedHashMap<>();
            mdc.forEach(mdcJson::put);
            json.put("mdc", new JsonObject(mdcJson));
        }

        // Key-value pairs
        List<KeyValuePair> kvps = LogEventFormattingSupport.sortedKeyValuePairs(event.getKeyValuePairs());
        if (!kvps.isEmpty()) {
            JsonObject kvpJson = new JsonObject();
            for (KeyValuePair kv : kvps) {
                kvpJson.put(kv.key, kv.value);
            }
            json.put("kvp", kvpJson);
        }

        // Exception
        if (event.getThrowable() != null) {
            StringWriter sw = new StringWriter();
            event.getThrowable().printStackTrace(new PrintWriter(sw));
            json.put("exception", sw.toString());
        }

        return json.encode();
    }
}
