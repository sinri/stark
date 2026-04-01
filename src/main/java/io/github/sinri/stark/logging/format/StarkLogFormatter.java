package io.github.sinri.stark.logging.format;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;

/**
 * Formats a {@link StarkLoggingEvent} into a string for output.
 * <p>
 * Built-in implementations: {@link TextLogFormatter} and {@link JsonLogFormatter}.
 * Users can provide custom implementations via the system property
 * {@code stark.logging.formatter.class}.
 */
public interface StarkLogFormatter {
    String format(StarkLoggingEvent event);
}
