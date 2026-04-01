package io.github.sinri.stark.logging.sink;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;

/**
 * Consumes rendered logging output.
 * <p>
 * This abstraction decouples event formatting from the final output transport.
 */
public interface StarkLogSink {
    void write(StarkLoggingEvent event, String renderedEvent);
}
