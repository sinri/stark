package io.github.sinri.stark.logging.base;

public interface LogRender<R> {
    R render(String topic, Log log);
}
