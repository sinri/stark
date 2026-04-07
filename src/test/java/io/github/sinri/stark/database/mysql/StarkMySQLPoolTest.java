package io.github.sinri.stark.database.mysql;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StarkMySQLPoolTest {

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close().onComplete(ar -> latch.countDown());
        assertTrueAwait(latch);
    }

    private static void assertTrueAwait(CountDownLatch latch) throws InterruptedException {
        org.junit.jupiter.api.Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private static SqlConnectOptions localOptions(int port) {
        return new SqlConnectOptions()
                .setHost("127.0.0.1")
                .setPort(port)
                .setDatabase("test")
                .setUser("u")
                .setPassword("p");
    }

    @Test
    void createRegistersPoolCloseRemovesFromRegistry() throws Exception {
        SqlConnectOptions opts = localOptions(63079);
        Pool pool = StarkMySQLPool.create(vertx, "reg-one", opts, new PoolOptions().setMaxSize(1));
        assertSame(pool, StarkMySQLPool.registered("reg-one"));
        pool.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertNull(StarkMySQLPool.registered("reg-one"));
    }

    @Test
    void duplicatePoolNameThrows() throws Exception {
        SqlConnectOptions opts = localOptions(63080);
        Pool first = StarkMySQLPool.create(vertx, "dup-name", opts, new PoolOptions().setMaxSize(1));
        assertThrows(IllegalStateException.class, () ->
                StarkMySQLPool.create(vertx, "dup-name", opts, new PoolOptions().setMaxSize(1)));
        first.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void unregisterUnknownReturnsNull() {
        assertNull(StarkMySQLPool.unregister("no-such-pool-" + System.nanoTime()));
    }

    @Test
    void unregisterReturnsPreviousPool() throws Exception {
        SqlConnectOptions opts = localOptions(63081);
        Pool pool = StarkMySQLPool.create(vertx, "unreg", opts, new PoolOptions().setMaxSize(1));
        assertSame(pool, StarkMySQLPool.unregister("unreg"));
        assertNull(StarkMySQLPool.registered("unreg"));
        pool.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void registerNullPoolNameThrows() throws Exception {
        String name = "holder-" + System.nanoTime();
        Pool pool = StarkMySQLPool.create(vertx, name, localOptions(63082), new PoolOptions().setMaxSize(1));
        try {
            assertThrows(NullPointerException.class, () -> StarkMySQLPool.register(null, pool));
        } finally {
            pool.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
