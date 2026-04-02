package io.github.sinri.stark.logging.base;

public interface Logger {
    String topic();
    void log(Log log);

    // todo: more log level
}
