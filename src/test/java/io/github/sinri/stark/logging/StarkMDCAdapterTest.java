package io.github.sinri.stark.logging;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class StarkMDCAdapterTest {

    @Test
    void putNullRemovesExistingValue() {
        StarkMDCAdapter adapter = new StarkMDCAdapter();

        adapter.put("traceId", "trace-1");
        adapter.put("traceId", null);

        Assertions.assertNull(adapter.get("traceId"));
        Assertions.assertNull(adapter.getCopyOfContextMap());
    }

    @Test
    void setContextMapFiltersNullValues() {
        StarkMDCAdapter adapter = new StarkMDCAdapter();
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("requestId", "req-1");
        contextMap.put("traceId", null);

        adapter.setContextMap(contextMap);

        Assertions.assertEquals("req-1", adapter.get("requestId"));
        Assertions.assertNull(adapter.get("traceId"));
        Assertions.assertEquals(Map.of("requestId", "req-1"), adapter.getCopyOfContextMap());
    }

    @Test
    void clearRemovesThreadLocalState() {
        StarkMDCAdapter adapter = new StarkMDCAdapter();

        adapter.put("traceId", "trace-1");
        adapter.pushByKey("stack", "v1");
        adapter.clear();

        Assertions.assertNull(adapter.get("traceId"));
        Assertions.assertNull(adapter.getCopyOfContextMap());
        Assertions.assertNull(adapter.popByKey("stack"));
    }

    @Test
    void keepsMdcAcrossVertxContextCallbacks() throws Exception {
        Vertx vertx = Vertx.vertx();
        StarkMDCAdapter adapter = new StarkMDCAdapter();
        CompletableFuture<List<Map<String, String>>> snapshotsFuture = new CompletableFuture<>();
        Context context = vertx.getOrCreateContext();

        context.runOnContext(ignored -> {
            ArrayList<Map<String, String>> snapshots = new ArrayList<>();
            adapter.put("traceId", "trace-ctx");
            snapshots.add(adapter.getCopyOfContextMap());
            context.runOnContext(inner -> {
                snapshots.add(adapter.getCopyOfContextMap());
                snapshotsFuture.complete(snapshots);
            });
        });

        try {
            List<Map<String, String>> snapshots = snapshotsFuture.get(5, TimeUnit.SECONDS);
            Assertions.assertEquals(List.of(
                    Map.of("traceId", "trace-ctx"),
                    Map.of("traceId", "trace-ctx")
            ), snapshots);
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        }
    }
}
