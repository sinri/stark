package io.github.sinri.stark.database.mysql;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;

public interface StarkMySQLPool extends Pool {
    /**
     * Like {@link #pool(SqlConnectOptions, PoolOptions)} with default options.
     */
    static Pool pool(SqlConnectOptions connectOptions) {
        return pool(connectOptions, new PoolOptions());
    }

    /**
     * Like {@link #pool(Vertx, SqlConnectOptions, PoolOptions)} with a Vert.x instance created automatically.
     */
    static Pool pool(SqlConnectOptions database, PoolOptions options) {
        Context context = Vertx.currentContext();
        if (context != null) {
            return pool(context.owner(), database, options);
        }
        throw new IllegalStateException("Vert.x is not initialized.");
    }

    static Pool pool(Vertx vertx, SqlConnectOptions database, PoolOptions options) {
        return new StarkMySQLPoolImpl(vertx, database, options);
    }
}
