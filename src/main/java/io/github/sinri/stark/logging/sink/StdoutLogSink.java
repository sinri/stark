package io.github.sinri.stark.logging.sink;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;

/**
 * Default sink that writes rendered events to {@link System#out}.
 */
public class StdoutLogSink implements StarkLogSink {
    @Override
    public void write(StarkLoggingEvent event, String renderedEvent) {
        System.out.println(renderedEvent);
    }
}
