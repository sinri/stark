package io.github.sinri.stark.logging.base;

import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

public interface LogProcesser {
    /**
     * Process the log, synchronously or asynchronously.
     * <p>
     * This function is not expected to throw any exception,
     * and it should handle log processing in a thread-safe manner.
     *
     * @param log the log to be processed
     * @return a Future that completes when the log is processed async, or null if the log is processed synchronously.
     */
    @Nullable Future<Void> process(String topic,Log log);

}
