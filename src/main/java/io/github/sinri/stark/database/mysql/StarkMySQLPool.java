package io.github.sinri.stark.database.mysql;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import org.jspecify.annotations.Nullable;

/**
 * Reactive MySQL {@link Pool} with a static registry keyed by application-defined names.
 * <p>
 * {@link #create} registers the pool under {@code poolName} and {@link #close()} on the returned
 * instance is idempotent: it removes that name from the registry (if still mapped to this pool)
 * and closes the underlying client once.
 * <p>
 * To replace a pool under an existing name, {@linkplain #unregister unregister} (and close the
 * previous pool if needed) before {@linkplain #register registering} or {@linkplain #create creating}
 * again.
 */
public interface StarkMySQLPool extends Pool {

    static @Nullable Pool registered(String poolName) {
        return StarkMySQLPoolImpl.POOL_MAP.get(poolName);
    }

    /**
     * Registers {@code pool} under {@code poolName}.
     * <p>
     * Registering the same {@code pool} instance again under the same name is a no-op.
     * <p>
     * If another pool is already registered under {@code poolName}, throws {@link IllegalStateException}.
     */
    static void register(String poolName, Pool pool) {
        StarkMySQLPoolImpl.registerPool(poolName, pool);
    }

    /**
     * Removes the mapping for {@code poolName} and returns the previously registered pool, or {@code null}.
     */
    static @Nullable Pool unregister(String poolName) {
        return StarkMySQLPoolImpl.unregisterPool(poolName);
    }

    static Pool create(Vertx vertx, String poolName, SqlConnectOptions sqlConnectOptions, PoolOptions options) {
        Pool pool = new StarkMySQLPoolImpl(vertx, poolName, sqlConnectOptions, options);
        register(poolName, pool);
        return pool;
    }
}
