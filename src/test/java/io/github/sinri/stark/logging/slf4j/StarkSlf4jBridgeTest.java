package io.github.sinri.stark.logging.slf4j;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

class StarkSlf4jBridgeTest {
    private final LoggerFactory originalFactory = LoggerFactory.universal();

    @AfterEach
    void restoreUniversalFactory() {
        LoggerFactory.universal(originalFactory);
    }

    @Test
    void loggerFactoryCachesByName() {
        StarkSlf4jLoggerFactory factory = new StarkSlf4jLoggerFactory();
        org.slf4j.Logger a1 = factory.getLogger("A");
        org.slf4j.Logger a2 = factory.getLogger("A");
        org.slf4j.Logger b = factory.getLogger("B");

        assertSame(a1, a2);
        assertNotSame(a1, b);
    }

    @Test
    void serviceProviderInitializesFactories() {
        StarkSlf4jServiceProvider provider = new StarkSlf4jServiceProvider();
        provider.initialize();

        assertNotNull(provider.getLoggerFactory());
        assertNotNull(provider.getMarkerFactory());
        assertNotNull(provider.getMDCAdapter());
        assertEquals(StarkSlf4jServiceProvider.REQUESTED_API_VERSION, provider.getRequestedApiVersion());
    }

    @Test
    void slf4jFormattedMessageIsMappedToStarkLog() {
        CapturingFactory capturingFactory = new CapturingFactory();
        LoggerFactory.universal(capturingFactory);

        org.slf4j.Logger logger = new StarkSlf4jLogger("topic.format");
        logger.info("hello {}, number {}", "world", 7);

        CapturingLogger topicLogger = capturingFactory.get("topic.format");
        assertNotNull(topicLogger);
        assertEquals(1, topicLogger.logs.size());

        Log log = topicLogger.logs.getFirst();
        assertEquals("INFO", log.getLevel());
        assertEquals("hello world, number 7", log.getMessage());
        assertNull(log.getThrowable());
    }

    @Test
    void throwableArgumentIsMappedToStarkLog() {
        CapturingFactory capturingFactory = new CapturingFactory();
        LoggerFactory.universal(capturingFactory);

        org.slf4j.Logger logger = new StarkSlf4jLogger("topic.throwable");
        IllegalStateException ex = new IllegalStateException("boom");
        logger.warn("warn with ex", ex);

        CapturingLogger topicLogger = capturingFactory.get("topic.throwable");
        assertNotNull(topicLogger);
        assertEquals(1, topicLogger.logs.size());

        Log log = topicLogger.logs.getFirst();
        assertEquals("WARN", log.getLevel());
        assertEquals("warn with ex", log.getMessage());
        assertSame(ex, log.getThrowable());
    }

    @Test
    void allLevelsAreEnabled() {
        org.slf4j.Logger logger = new StarkSlf4jLogger("topic.enabled");
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
    }

    private static class CapturingFactory implements LoggerFactory {
        private final ConcurrentMap<String, CapturingLogger> map = new ConcurrentHashMap<>();

        @Override
        public Logger createLogger(String topic) {
            return map.computeIfAbsent(topic, CapturingLogger::new);
        }

        private CapturingLogger get(String topic) {
            return map.get(topic);
        }
    }

    private static class CapturingLogger implements Logger {
        private final String topic;
        private final List<Log> logs = new ArrayList<>();

        private CapturingLogger(String topic) {
            this.topic = topic;
        }

        @Override
        public String topic() {
            return topic;
        }

        @Override
        public void log(Log log) {
            logs.add(Objects.requireNonNull(log));
        }
    }
}

