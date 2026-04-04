package io.github.sinri.stark.logging.base.impl.factory;

import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import io.github.sinri.stark.logging.base.impl.logger.StdoutLogger;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StdoutLoggerFactory implements LoggerFactory {
    private final StdoutLogProcesser logProcesser;
    private final Map<String, Logger> loggerCache = new ConcurrentHashMap<>();

    public StdoutLoggerFactory(StdoutLogProcesser logProcesser) {
        this.logProcesser = logProcesser;
    }

    @Override
    public Logger createLogger(String topic) {
        return loggerCache.computeIfAbsent(topic, _ -> new StdoutLogger(topic) {
            @Override
            protected LogProcesser getLogProcesser() {
                return logProcesser;
            }
        });
    }
}
