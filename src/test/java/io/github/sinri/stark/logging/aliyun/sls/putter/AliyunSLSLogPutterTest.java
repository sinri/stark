package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.logging.aliyun.sls.putter.entity.LogGroup;
import io.github.sinri.stark.logging.aliyun.sls.putter.entity.LogItem;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AliyunSLSLogPutterTest {

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void blankSourceExpressionYieldsEmptySource() throws Exception {
        AliyunSLSLogPutter putter = new AliyunSLSLogPutter(vertx, "ak", "sk", "example.com", null);
        assertEquals("", putter.getSource());
        putter.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void ipPlaceholderReplacedOrEmptyWhenLocalHostUnknown() throws Exception {
        AliyunSLSLogPutter putter = new AliyunSLSLogPutter(vertx, "ak", "sk", "example.com", "n-[IP]-x");
        String source = putter.getSource();
        assertTrue(source.isEmpty() || (source.startsWith("n-") && source.endsWith("-x")));
        putter.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    void putLogsCompletesSuccessfullyOnNetworkFailure() throws Exception {
        AliyunSLSLogPutter putter = new AliyunSLSLogPutter(vertx, "ak", "sk", "unresolvable-host.invalid", null);
        LogGroup group = new LogGroup("t", "");
        group.addLogItem(new LogItem(1).addContent("k", "v"));
        putter.putLogs("proj", "store", group).toCompletionStage().toCompletableFuture().get(60, TimeUnit.SECONDS);
        putter.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
}
