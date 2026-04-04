package io.github.sinri.stark.logging.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StarkSlf4jLoggerFactory implements ILoggerFactory {
    private final Map<String, Logger> loggerMap = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, StarkSlf4jLogger::new);
    }
}

