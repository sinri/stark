package io.github.sinri.stark.logging;

import io.vertx.core.internal.VertxBootstrap;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.core.spi.context.storage.ContextLocal;

import java.util.Deque;
import java.util.Map;

/**
 * Registers {@link ContextLocal} keys used by {@link StarkMDCAdapter}
 * for Vert.x context-aware MDC storage.
 * <p>
 * This provider is loaded via {@code META-INF/services/io.vertx.core.spi.VertxServiceProvider}
 * and runs before the {@link Vertx} instance is created, which is when
 * {@link ContextLocal#registerLocal(Class)} must be called.
 */
public class StarkLoggingVertxServiceProvider implements VertxServiceProvider {

    @SuppressWarnings("unchecked")
    static final ContextLocal<Map<String, String>> MDC_MAP =
            ContextLocal.registerLocal((Class<Map<String, String>>) (Class<?>) Map.class);

    @SuppressWarnings("unchecked")
    static final ContextLocal<Map<String, Deque<String>>> MDC_DEQUE_MAP =
            ContextLocal.registerLocal((Class<Map<String, Deque<String>>>) (Class<?>) Map.class);

    @Override
    public void init(VertxBootstrap builder) {
        // ContextLocal keys are registered via static field initializers above.
        // No additional bootstrap configuration is needed.
    }
}
