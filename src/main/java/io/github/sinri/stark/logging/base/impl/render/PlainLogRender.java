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
    public static final PlainLogRender INSTANCE = new PlainLogRender();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

    private PlainLogRender() {
    }

    /**
     * 渲染日志的分类信息为字符串。
     *
     * @param classification 日志的分类信息
     * @return 渲染后的分类信息字符串
     */
    public String renderClassification(List<String> classification) {
        return String.join(",", classification);
    }

    /**
     * 渲染日志的异常信息为字符串。
     *
     * @param throwable 日志对应的异常对象
     * @return 渲染后的异常信息字符串
     */
    public String renderThrowable(Throwable throwable) {
        return ThrowableRender.renderThrowableChain(throwable);
    }

    /**
     * 渲染日志的上下文信息为字符串。
     *
     * @param context 日志的上下文信息
     * @return 渲染后的上下文信息字符串
     */
    public String renderContext(Map<String, @Nullable Object> context) {
        return context.entrySet().stream()
                      .map(entry -> "\t%s:\t%s".formatted(entry.getKey(), entry.getValue()))
                      .collect(Collectors.joining("\n"));
    }

    /**
     * 渲染日志为字符串。
     *
     * @param topic 主题
     * @param log   日志
     * @return 日志经渲染后的字符串
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
