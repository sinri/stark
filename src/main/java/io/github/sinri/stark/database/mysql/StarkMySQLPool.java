package io.github.sinri.stark.database.mysql;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import org.jspecify.annotations.Nullable;

public interface StarkMySQLPool extends Pool {

    static @Nullable Pool registered(String poolName) {
        return StarkMySQLPoolImpl.POOL_MAP.get(poolName);
    }

    static void register(String poolName, Pool pool) {
        StarkMySQLPoolImpl.POOL_MAP.put(poolName, pool);
    }

    static Pool create(Vertx vertx, String poolName, SqlConnectOptions sqlConnectOptions, PoolOptions options) {
        Pool pool = new StarkMySQLPoolImpl(vertx, sqlConnectOptions, options);
        register(poolName, pool);
        return pool;
    }
}
