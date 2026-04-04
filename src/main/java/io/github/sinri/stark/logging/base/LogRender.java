package io.github.sinri.stark.logging.base;

public interface LogRender<R> {
    R render(String topic, Log log);

    default String renderToString(String topic, Log log) {
        return render(topic, log).toString();
    }
}
