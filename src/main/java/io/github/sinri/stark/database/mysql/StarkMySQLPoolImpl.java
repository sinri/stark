package io.github.sinri.stark.database.mysql;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.sqlclient.*;

class StarkMySQLPoolImpl implements StarkMySQLPool {
    private final Pool embeddedPool;

    public StarkMySQLPoolImpl(Vertx vertx, SqlConnectOptions connectOptions, PoolOptions poolOptions) {
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
        return getEmbeddedPool().close();
    }

    @Override
    public int size() {
        return getEmbeddedPool().size();
    }
}
