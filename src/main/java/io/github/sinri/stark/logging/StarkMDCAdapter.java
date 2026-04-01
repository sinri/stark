package io.github.sinri.stark.logging;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.jspecify.annotations.Nullable;
import org.slf4j.spi.MDCAdapter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dual-mode MDC (Mapped Diagnostic Context) adapter.
 * <p>
 * When running inside a Vert.x context ({@link Vertx#currentContext()} is non-null),
 * MDC data is stored on the Vert.x {@link Context} via {@link io.vertx.core.spi.context.storage.ContextLocal},
 * which correctly follows the request lifecycle across async callbacks on the event loop.
 * <p>
 * When running outside a Vert.x context (plain threads, tests, CLI tools),
 * falls back to {@link ThreadLocal} storage.
 */
public class StarkMDCAdapter implements MDCAdapter {

    private final ThreadLocal<@Nullable Map<String, String>> mapThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<@Nullable Map<String, Deque<String>>> dequeThreadLocal = new ThreadLocal<>();

    // --- storage dispatch ---

    private Map<String, String> getMap() {
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            return ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_MAP, ConcurrentHashMap::new);
        }
        Map<String, String> map = mapThreadLocal.get();
        if (map == null) {
            map = new HashMap<>();
            mapThreadLocal.set(map);
        }
        return map;
    }

    private @Nullable Map<String, String> getMapOrNull() {
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            return ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_MAP);
        }
        return mapThreadLocal.get();
    }

    private Map<String, Deque<String>> getDequeMap() {
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            return ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_DEQUE_MAP, ConcurrentHashMap::new);
        }
        Map<String, Deque<String>> map = dequeThreadLocal.get();
        if (map == null) {
            map = new HashMap<>();
            dequeThreadLocal.set(map);
        }
        return map;
    }

    private @Nullable Map<String, Deque<String>> getDequeMapOrNull() {
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            return ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_DEQUE_MAP);
        }
        return dequeThreadLocal.get();
    }

    // --- MDCAdapter implementation ---

    @Override
    public void put(String key, @Nullable String val) {
        if (val == null) {
            remove(key);
            return;
        }
        getMap().put(key, val);
    }

    @Override
    public @Nullable String get(String key) {
        Map<String, String> map = getMapOrNull();
        return map != null ? map.get(key) : null;
    }

    @Override
    public void remove(String key) {
        Map<String, String> map = getMapOrNull();
        if (map != null) {
            map.remove(key);
        }
    }

    @Override
    public void clear() {
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            Map<String, String> map = ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_MAP);
            if (map != null) {
                map.clear();
            }
            Map<String, Deque<String>> dequeMap = ctx.getLocal(StarkLoggingVertxServiceProvider.MDC_DEQUE_MAP);
            if (dequeMap != null) {
                dequeMap.clear();
            }
            return;
        }

        mapThreadLocal.remove();
        dequeThreadLocal.remove();
    }

    @Override
    public @Nullable Map<String, String> getCopyOfContextMap() {
        Map<String, String> map = getMapOrNull();
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new HashMap<>(map);
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        Map<String, String> sanitizedContextMap = sanitizeContextMap(contextMap);
        Context ctx = Vertx.currentContext();
        if (ctx != null) {
            ctx.putLocal(StarkLoggingVertxServiceProvider.MDC_MAP, new ConcurrentHashMap<>(sanitizedContextMap));
        } else {
            mapThreadLocal.set(new HashMap<>(sanitizedContextMap));
        }
    }

    @Override
    public void pushByKey(String key, String value) {
        getDequeMap().computeIfAbsent(key, k -> new ArrayDeque<>()).push(value);
    }

    @Override
    public @Nullable String popByKey(String key) {
        Map<String, Deque<String>> dequeMap = getDequeMapOrNull();
        if (dequeMap == null) {
            return null;
        }
        Deque<String> deque = dequeMap.get(key);
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        return deque.pop();
    }

    @Override
    public @Nullable Deque<String> getCopyOfDequeByKey(String key) {
        Map<String, Deque<String>> dequeMap = getDequeMapOrNull();
        if (dequeMap == null) {
            return null;
        }
        Deque<String> deque = dequeMap.get(key);
        if (deque == null) {
            return null;
        }
        return new ArrayDeque<>(deque);
    }

    @Override
    public void clearDequeByKey(String key) {
        Map<String, Deque<String>> dequeMap = getDequeMapOrNull();
        if (dequeMap == null) {
            return;
        }
        Deque<String> deque = dequeMap.get(key);
        if (deque != null) {
            deque.clear();
        }
    }

    private Map<String, String> sanitizeContextMap(Map<String, String> contextMap) {
        LinkedHashMap<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            if (entry.getValue() != null) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }
}
