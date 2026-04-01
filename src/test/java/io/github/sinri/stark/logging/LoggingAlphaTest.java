package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.format.JsonLogFormatter;
import io.github.sinri.stark.logging.format.TextLogFormatter;
import io.github.sinri.stark.logging.sink.StdoutLogSink;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LoggingAlphaTest {

    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;
        MDC.clear();
        StarkLoggerFactory.installed()
                          .setFormatter(new TextLogFormatter())
                          .setSink(new StdoutLogSink())
                          .clearLevelOverrides()
                          .setDefaultLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void logsToStdout() {
        ConsoleCapture capture = captureConsole(() ->
                LoggerFactory.getLogger(LoggingAlphaTest.class).info("Hello World!"));

        Assertions.assertTrue(capture.stdout().contains("Hello World!"));
        Assertions.assertTrue(capture.stderr().isEmpty());
    }

    @Test
    void supportsRuntimeFormatterSwitchAndLevelFiltering() {
        Logger logger = LoggerFactory.getLogger(LoggingAlphaTest.class);
        ConsoleCapture capture = captureConsole(() -> {
            logger.info("this is a info log before");
            logger.warn("this is a warn log before");
            logger.error("this is a error log before");

            StarkLoggerFactory.installed()
                              .setFormatter(new JsonLogFormatter())
                              .setDefaultLevel(Level.WARN);

            logger.info("this is a info log after");
            logger.warn("this is a warn log after");
            logger.error("this is a error log after");
        });

        Assertions.assertTrue(capture.stdout().contains("this is a info log before"));
        Assertions.assertTrue(capture.stdout().contains("this is a warn log before"));
        Assertions.assertTrue(capture.stdout().contains("this is a error log before"));
        Assertions.assertFalse(capture.stdout().contains("this is a info log after"));
        Assertions.assertTrue(capture.stdout().contains("\"message\":\"this is a warn log after\""));
        Assertions.assertTrue(capture.stdout().contains("\"message\":\"this is a error log after\""));
        Assertions.assertTrue(capture.stderr().isEmpty());
    }

    @Test
    void rendersTextOutputWithMdcMarkersKeyValuesAndThrowable() {
        Logger logger = LoggerFactory.getLogger(LoggingAlphaTest.class);
        ConsoleCapture capture = captureConsole(() -> {
            MDC.put("traceId", "trace-1");
            MDC.put("requestId", "req-1");

            logger.atWarn()
                  .addMarker(MarkerFactory.getMarker("beta"))
                  .addMarker(MarkerFactory.getMarker("alpha"))
                  .addKeyValue("z", "9")
                  .addKeyValue("a", "1")
                  .setCause(new IllegalStateException("boom"))
                  .log("Text event");

            MDC.put("traceId", "trace-2");
            logger.info("Snapshot event");
        });

        Assertions.assertTrue(capture.stdout().contains("Text event"));
        Assertions.assertTrue(capture.stdout().contains("  markers: alpha beta"));
        Assertions.assertTrue(capture.stdout().contains("  mdc: requestId=req-1 traceId=trace-1"));
        Assertions.assertTrue(capture.stdout().contains("  kvp: a=1 z=9"));
        Assertions.assertTrue(capture.stdout().contains("java.lang.IllegalStateException: boom"));
        Assertions.assertTrue(capture.stdout().contains("  mdc: requestId=req-1 traceId=trace-2"));
        Assertions.assertTrue(capture.stderr().isEmpty());
    }

    @Test
    void rendersJsonOutputWithSortedMarkersMdcAndKeyValuePairs() {
        Logger logger = LoggerFactory.getLogger(LoggingAlphaTest.class);
        StarkLoggerFactory.installed()
                          .setFormatter(new JsonLogFormatter())
                          .setDefaultLevel(Level.INFO);

        ConsoleCapture capture = captureConsole(() -> {
            MDC.put("traceId", "trace-1");
            MDC.put("requestId", "req-1");

            logger.atInfo()
                  .addMarker(MarkerFactory.getMarker("beta"))
                  .addMarker(MarkerFactory.getMarker("alpha"))
                  .addKeyValue("z", "9")
                  .addKeyValue("a", "1")
                  .setCause(new IllegalArgumentException("bad json"))
                  .log("Json event");
        });

        String line = capture.stdout().trim();
        JsonObject json = new JsonObject(line);
        Assertions.assertEquals("Json event", json.getString("message"));
        Assertions.assertEquals(List.of("alpha", "beta"), json.getJsonArray("markers").getList());
        Assertions.assertEquals("req-1", json.getJsonObject("mdc").getString("requestId"));
        Assertions.assertEquals("trace-1", json.getJsonObject("mdc").getString("traceId"));
        Assertions.assertEquals("1", json.getJsonObject("kvp").getString("a"));
        Assertions.assertEquals("9", json.getJsonObject("kvp").getString("z"));
        Assertions.assertTrue(json.getString("exception").contains("bad json"));
        Assertions.assertTrue(line.contains("\"markers\":[\"alpha\",\"beta\"]"));
        Assertions.assertTrue(line.contains("\"mdc\":{\"requestId\":\"req-1\",\"traceId\":\"trace-1\"}"));
        Assertions.assertTrue(line.contains("\"kvp\":{\"a\":\"1\",\"z\":\"9\"}"));
        Assertions.assertTrue(capture.stderr().isEmpty());
    }

    private ConsoleCapture captureConsole(Runnable runnable) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
        try {
            runnable.run();
        } finally {
            System.out.flush();
            System.err.flush();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return new ConsoleCapture(
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8)
        );
    }

    private record ConsoleCapture(String stdout, String stderr) {
    }
}
