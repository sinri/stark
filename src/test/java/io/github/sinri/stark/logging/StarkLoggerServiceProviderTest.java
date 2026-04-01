package io.github.sinri.stark.logging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class StarkLoggerServiceProviderTest {

    @Test
    void writesConfigurationDiagnosticsToStderr() {
        String originalFormatterClass = System.getProperty(StarkLoggingConfiguration.FORMATTER_CLASS_PROPERTY);
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try {
            System.setProperty(StarkLoggingConfiguration.FORMATTER_CLASS_PROPERTY, BrokenFormatter.class.getName());
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));

            new StarkLoggerServiceProvider().initialize();

            Assertions.assertTrue(stdout.toString(StandardCharsets.UTF_8).isEmpty());
            Assertions.assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("failed to load custom formatter"));
        } finally {
            if (originalFormatterClass == null) {
                System.clearProperty(StarkLoggingConfiguration.FORMATTER_CLASS_PROPERTY);
            } else {
                System.setProperty(StarkLoggingConfiguration.FORMATTER_CLASS_PROPERTY, originalFormatterClass);
            }
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    public static final class BrokenFormatter {
        public BrokenFormatter(String ignored) {
        }
    }
}
