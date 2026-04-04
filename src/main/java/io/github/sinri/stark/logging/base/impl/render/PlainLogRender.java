package io.github.sinri.stark.logging.base.impl.render;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogRender;
import io.github.sinri.stark.logging.base.impl.ThrowableRender;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlainLogRender implements LogRender<String> {
    // public static final PlainLogRender INSTANCE = new PlainLogRender();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    public PlainLogRender() {
    }

    /**
     * Render log classification as a string.
     *
     * @param classification the log classification list
     * @return the rendered classification string
     */
    public String renderClassification(List<String> classification) {
        return String.join(",", classification);
    }

    /**
     * Render a throwable as a string.
     *
     * @param throwable the throwable to render
     * @return the rendered throwable string
     */
    public String renderThrowable(Throwable throwable) {
        return ThrowableRender.renderThrowableChain(throwable);
    }

    /**
     * Render log context as a string.
     *
     * @param context the log context map
     * @return the rendered context string
     */
    public String renderContext(Map<String, @Nullable Object> context) {
        return context.entrySet().stream()
                      .map(entry -> "\t%s:\t%s".formatted(entry.getKey(), entry.getValue()))
                      .collect(Collectors.joining("\n"));
    }

    /**
     * Render a log entry as a plain text string.
     *
     * @param topic the log topic
     * @param log   the log entry
     * @return the rendered log string
     */
    @Override
    public String render(String topic, Log log) {
        StringBuilder sb = new StringBuilder();
        var zonedDateTime = Instant.ofEpochMilli(log.getTimestamp()).atZone(ZoneId.systemDefault());
        sb.append("* ")
          .append(zonedDateTime.format(formatter)).append(" ")
          .append("[").append(log.getLevel()).append("] ")
          .append(topic)
          .append(" <").append(log.getThread()).append(">");

        //        List<String> classification = log.classification();
        //        if (classification != null && !classification.isEmpty()) {
        //            sb.append("\n").append(renderClassification(classification));
        //        }

        String message = log.getMessage();
        if (message != null && !message.isBlank()) {
            sb.append("\n").append(message);
        }
        Throwable exception = log.getThrowable();
        if (exception != null) {
            sb.append("\n")
              .append(renderThrowable(exception));
        }

        JsonObject context = log.getContext();
        if (context != null) {
            Map<String, @Nullable Object> map = context.getMap();
            if (!map.isEmpty()) {
                sb.append("\n")
                  .append(renderContext(map));
            }
        }

        Map<String, @Nullable Object> extra = log.getExtraItemMap();
        if (!extra.isEmpty()) {
            sb.append("\nExtra as following:\n")
              .append(renderContext(extra));
        }

        return sb.toString();
    }
}
