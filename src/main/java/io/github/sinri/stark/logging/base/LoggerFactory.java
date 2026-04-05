package io.github.sinri.stark.logging.base;

import io.github.sinri.stark.logging.base.impl.factory.StdoutLoggerFactory;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;

import java.util.concurrent.atomic.AtomicReference;

public interface LoggerFactory {
    AtomicReference<LoggerFactory> _UNIVERSAL_INSTANCE = new AtomicReference<>(new StdoutLoggerFactory(new StdoutLogProcesser()));

    static LoggerFactory universal() {
        return _UNIVERSAL_INSTANCE.get();
    }

    static void universal(LoggerFactory loggerFactory) {
        _UNIVERSAL_INSTANCE.set(loggerFactory);
    }

    Logger createLogger(String topic);

    default Logger createLogger(Class<?> clazz) {
        return createLogger(clazz.getName());
    }
}
