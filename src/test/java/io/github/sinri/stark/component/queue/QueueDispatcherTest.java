package io.github.sinri.stark.component.queue;

import io.github.sinri.stark.core.Stark;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueDispatcherTest {

    private Vertx vertx;
    private Stark stark;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        stark = new Stark(vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private static final class StopOnlyDispatcher extends QueueDispatcher {
        @Override
        public long getWaitingPeriodInMsWhenTaskFree() {
            return 50L;
        }

        @Override
        public Future<QueueSignal> readSignal() {
            return Future.succeededFuture(QueueSignal.STOP);
        }

        @Override
        public Future<QueueTask> seekNextTask() {
            return Future.succeededFuture(null);
        }
    }

    @Test
    void undeployTerminatesQueue() throws Exception {
        StopOnlyDispatcher dispatcher = new StopOnlyDispatcher();
        dispatcher.deployMe(stark).toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        Thread.sleep(150);
        dispatcher.undeployMe().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        assertEquals(QueueStatus.TERMINATED, dispatcher.getQueueStatus());
    }
}
