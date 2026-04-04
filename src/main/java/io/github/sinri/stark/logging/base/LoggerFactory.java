package io.github.sinri.stark.logging.base;

import io.github.sinri.stark.logging.base.impl.factory.StdoutLoggerFactory;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;

import java.util.concurrent.atomic.AtomicReference;

public interface LoggerFactory {
    AtomicReference<LoggerFactory> _UNIVERSAL_INSTANCE = new AtomicReference<>(new StdoutLoggerFactory(new StdoutLogProcesser()));

    static LoggerFactory getUniversalLoggerFactory() {
        return _UNIVERSAL_INSTANCE.get();
    }

    static void setUniversalLoggerFactory(LoggerFactory loggerFactory) {
        _UNIVERSAL_INSTANCE.set(loggerFactory);
    }

    Logger createLogger(String topic);
}
