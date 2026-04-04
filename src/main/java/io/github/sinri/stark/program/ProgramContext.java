package io.github.sinri.stark.program;

import io.github.sinri.stark.logging.base.LoggerFactory;
import io.github.sinri.stark.logging.base.impl.factory.StdoutLoggerFactory;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;
import io.vertx.sqlclient.Pool;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProgramContext {
    private final Map<String, Pool> poolMap = new ConcurrentHashMap<>();
    private LoggerFactory loggerFactory = new StdoutLoggerFactory(new StdoutLogProcesser());

    public final @Nullable Pool getMySQLPool(String poolName) {
        return poolMap.get(poolName);
    }

    public final void setMySQLPool(String poolName, Pool pool) {
        poolMap.put(poolName, pool);
    }

    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public ProgramContext setLoggerFactory(LoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
        return this;
    }
}
