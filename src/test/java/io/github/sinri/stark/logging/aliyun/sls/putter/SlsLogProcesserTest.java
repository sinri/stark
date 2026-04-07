package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Log;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;

class SlsLogProcesserTest {

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
    void processReturnsNull() {
        SlsLogProcesser p = new SlsLogProcesser(null, 10, "p", "s");
        assertNull(p.process("topic", new Log().setMessage("x")));
    }

    @Test
    void deployAndUndeployWithNullPutter() throws Exception {
        SlsLogProcesser p = new SlsLogProcesser(null, 10, "p", "s");
        p.deployMe(stark, new DeploymentOptions()).toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        p.process("t", new Log().setMessage("m"));
        p.undeployMe().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }
}
