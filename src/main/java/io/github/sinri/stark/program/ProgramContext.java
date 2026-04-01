package io.github.sinri.stark.program;

import io.vertx.sqlclient.Pool;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ProgramContext {
    private final Map<String, Pool> poolMap = new ConcurrentHashMap<>();

    public final @Nullable Pool getMySQLPool(String poolName) {
        return poolMap.get(poolName);
    }

    public final void setMySQLPool(String poolName, Pool pool) {
        poolMap.put(poolName, pool);
    }


}
