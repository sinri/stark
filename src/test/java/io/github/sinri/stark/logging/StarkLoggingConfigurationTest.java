package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.format.JsonLogFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.Map;

public class StarkLoggingConfigurationTest {

    @Test
    void fallsBackToBuiltInFormatterWhenCustomFormatterCannotBeLoaded() {
        StarkLoggingConfiguration configuration = StarkLoggingConfiguration.fromMaps(
                Map.of(
                        StarkLoggingConfiguration.FORMAT_PROPERTY, "json",
                        StarkLoggingConfiguration.FORMATTER_CLASS_PROPERTY, BrokenFormatter.class.getName()
                ),
                Map.of()
        );

        Assertions.assertInstanceOf(JsonLogFormatter.class, configuration.getFormatter());
        Assertions.assertFalse(configuration.getDiagnostics().isEmpty());
        Assertions.assertTrue(configuration.getDiagnostics().getFirst().contains("failed to load custom formatter"));
    }

    @Test
    void prefersSystemPropertiesAndParsesLoggerOverrides() {
        StarkLoggingConfiguration configuration = StarkLoggingConfiguration.fromMaps(
                Map.of(
                        StarkLoggingConfiguration.FORMAT_PROPERTY, "text",
                        StarkLoggingConfiguration.LEVEL_PROPERTY, "WARN",
                        StarkLoggingConfiguration.LEVELS_PROPERTY, "io.github.sinri=DEBUG, io.github.sinri.stark.logging=TRACE"
                ),
                Map.of(
                        StarkLoggingConfiguration.FORMAT_ENV, "json",
                        StarkLoggingConfiguration.LEVEL_ENV, "ERROR"
                )
        );

        Assertions.assertEquals(Level.WARN, configuration.getDefaultLevel());
        Assertions.assertEquals(Level.DEBUG, configuration.getLoggerLevels().get("io.github.sinri"));
        Assertions.assertEquals(Level.TRACE, configuration.getLoggerLevels().get("io.github.sinri.stark.logging"));
    }

    public static final class BrokenFormatter {
        public BrokenFormatter(String ignored) {
        }
    }
}
