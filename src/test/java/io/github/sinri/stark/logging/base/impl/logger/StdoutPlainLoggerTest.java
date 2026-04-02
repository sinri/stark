package io.github.sinri.stark.logging.base.impl.logger;

import io.github.sinri.stark.logging.base.Log;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class StdoutPlainLoggerTest {

    @Test
    void topicReturnsConstructorValue() {
        StdoutPlainLogger logger = new StdoutPlainLogger("MyTopic");
        assertEquals("MyTopic", logger.topic());
    }

    @Test
    void logDelegatesToProcesser() {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured));

            StdoutPlainLogger logger = new StdoutPlainLogger("DelegateTopic");
            Log log = new Log();
            log.setMessage("delegate test");
            logger.log(log);

            String output = captured.toString();
            assertTrue(output.contains("delegate test"), "should output the log message");
            assertTrue(output.contains("DelegateTopic"), "should output the topic");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void logWithDifferentLevels() {
        PrintStream originalOut = System.out;
        try {
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured));

            StdoutPlainLogger logger = new StdoutPlainLogger("LevelTest");

            Log errorLog = new Log();
            errorLog.setLevel("ERROR");
            errorLog.setMessage("error msg");
            logger.log(errorLog);

            Log debugLog = new Log();
            debugLog.setLevel("DEBUG");
            debugLog.setMessage("debug msg");
            logger.log(debugLog);

            String output = captured.toString();
            assertTrue(output.contains("[ERROR]"));
            assertTrue(output.contains("error msg"));
            assertTrue(output.contains("[DEBUG]"));
            assertTrue(output.contains("debug msg"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
