package io.github.sinri.stark.component.sundial;

import io.github.sinri.stark.core.Stark;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CronJobDispatcherLifecycleTest {

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

    @Test
    void deployAndUndeployRunsRefreshPlans() throws Exception {
        CronJobDispatcher dispatcher = new CronJobDispatcher() {
            @Override
            protected Future<@Nullable Collection<CronJobPlan>> fetchPlans() {
                return Future.succeededFuture(List.of());
            }
        };
        String depId = dispatcher.deployMe(stark).toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        assertFalse(depId.isEmpty());
        dispatcher.undeployMe().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }
}
