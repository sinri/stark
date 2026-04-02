package io.github.sinri.stark.logging.base.impl.processor;

import io.github.sinri.stark.logging.base.Log;
import io.vertx.core.Future;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class PlainLogProcesserTest {

    @Test
    void processReturnNullForSynchronous() {
        Log log = new Log();
        log.setMessage("sync test");

        Future<Void> result = PlainLogProcesser.INSTANCE.process("TestTopic", log);
        assertNull(result, "synchronous processing should return null");
    }

    @Test
    void processWritesToStdout() {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured));

            Log log = new Log();
            log.setMessage("captured message");
            PlainLogProcesser.INSTANCE.process("CapturedTopic", log);

            String output = captured.toString();
            assertTrue(output.contains("captured message"), "stdout should contain the log message");
            assertTrue(output.contains("CapturedTopic"), "stdout should contain the topic");
            assertTrue(output.contains("[INFO]"), "stdout should contain the default log level");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void singletonInstance() {
        assertSame(PlainLogProcesser.INSTANCE, PlainLogProcesser.INSTANCE);
    }
}
