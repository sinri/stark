package io.github.sinri.stark.database.mysql;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.sqlclient.*;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class StarkMySQLPoolImpl implements StarkMySQLPool {
    final static Map<String, Pool> POOL_MAP = new ConcurrentHashMap<>();
    private static final Object REGISTRY_LOCK = new Object();

    static void registerPool(String poolName, Pool pool) {
        Objects.requireNonNull(poolName, "poolName");
        Objects.requireNonNull(pool, "pool");
        synchronized (REGISTRY_LOCK) {
            Pool existing = POOL_MAP.get(poolName);
            if (existing == pool) {
                return;
            }
            if (existing != null) {
                throw new IllegalStateException("MySQL pool name already registered: " + poolName);
            }
            POOL_MAP.put(poolName, pool);
        }
    }

    static @Nullable Pool unregisterPool(String poolName) {
        Objects.requireNonNull(poolName, "poolName");
        synchronized (REGISTRY_LOCK) {
            return POOL_MAP.remove(poolName);
        }
    }

    private final Pool embeddedPool;
    private final String registrationName;
    private final AtomicBoolean closeOnce = new AtomicBoolean(false);

    public StarkMySQLPoolImpl(Vertx vertx, String poolName, SqlConnectOptions connectOptions, PoolOptions poolOptions) {
        this.registrationName = poolName;
        this.embeddedPool = MySQLBuilder.pool()
                                        .with(poolOptions)
                                        .connectingTo(connectOptions)
                                        .using(vertx)
                                        .build();
    }

    public Pool getEmbeddedPool() {
        return embeddedPool;
    }

    @Override
    public Future<SqlConnection> getConnection() {
        return getEmbeddedPool().getConnection();
    }

    @Override
    public Query<RowSet<Row>> query(String sql) {
        return getEmbeddedPool().query(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql) {
        return getEmbeddedPool().preparedQuery(sql);
    }

    @Override
    public PreparedQuery<RowSet<Row>> preparedQuery(String sql, PrepareOptions options) {
        return getEmbeddedPool().preparedQuery(sql, options);
    }

    @Override
    public Future<Void> close() {
        if (!closeOnce.compareAndSet(false, true)) {
            return Future.succeededFuture();
        }
        POOL_MAP.remove(registrationName, this);
        return getEmbeddedPool().close();
    }

    @Override
    public int size() {
        return getEmbeddedPool().size();
    }
}
