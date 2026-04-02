package io.github.sinri.stark.logging.base;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogTest {

    @Test
    void constructorSetsDefaults() {
        long before = System.currentTimeMillis();
        Log log = new Log();
        long after = System.currentTimeMillis();

        assertTrue(log.getTimestamp() >= before && log.getTimestamp() <= after,
                "timestamp should be around current time");
        assertEquals("INFO", log.getLevel());
        assertEquals("", log.getMessage());
        assertNotNull(log.getThread());
        assertFalse(log.getThread().isBlank());
        assertNull(log.getContext());
        assertNull(log.getThrowable());
    }

    @Test
    void setAndGetMessage() {
        Log log = new Log();
        Log returned = log.setMessage("hello");
        assertSame(log, returned, "setMessage should return this");
        assertEquals("hello", log.getMessage());
    }

    @Test
    void setAndGetLevel() {
        Log log = new Log();
        log.setLevel("ERROR");
        assertEquals("ERROR", log.getLevel());
    }

    @Test
    void setAndGetContext() {
        Log log = new Log();
        JsonObject ctx = new JsonObject().put("key", "value");
        log.setContext(ctx);
        assertNotNull(log.getContext());
        assertEquals("value", log.getContext().getString("key"));
    }

    @Test
    void setContextNull() {
        Log log = new Log();
        log.setContext(new JsonObject());
        log.setContext(null);
        assertNull(log.getContext());
    }

    @Test
    void setAndGetThrowable() {
        Log log = new Log();
        RuntimeException ex = new RuntimeException("test");
        log.setThrowable(ex);
        assertSame(ex, log.getThrowable());
    }

    @Test
    void setThrowableNull() {
        Log log = new Log();
        log.setThrowable(new RuntimeException());
        log.setThrowable(null);
        assertNull(log.getThrowable());
    }

    @Test
    void setExtraItemSucceeds() {
        Log log = new Log();
        log.setExtraItem("customKey", 42);
        assertEquals(42, log.getValue("customKey"));
    }

    @Test
    void setExtraItemReturnsThis() {
        Log log = new Log();
        assertSame(log, log.setExtraItem("k", "v"));
    }

    @Test
    void setExtraItemRejectsReservedKeys() {
        Log log = new Log();
        for (String reserved : new String[]{Log.TIMESTAMP, Log.LEVEL, Log.MESSAGE, Log.THREAD, Log.CONTEXT}) {
            assertThrows(IllegalArgumentException.class, () -> log.setExtraItem(reserved, "x"),
                    "Should reject reserved key: " + reserved);
        }
    }

    @Test
    void getExtraItemMapFiltersReservedKeys() {
        Log log = new Log();
        log.setExtraItem("extra1", "a");
        log.setExtraItem("extra2", "b");

        Map<String, ?> extras = log.getExtraItemMap();
        assertEquals(2, extras.size());
        assertEquals("a", extras.get("extra1"));
        assertEquals("b", extras.get("extra2"));

        // reserved keys must not appear
        assertFalse(extras.containsKey(Log.TIMESTAMP));
        assertFalse(extras.containsKey(Log.LEVEL));
        assertFalse(extras.containsKey(Log.MESSAGE));
        assertFalse(extras.containsKey(Log.THREAD));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void getExtraItemMapIsUnmodifiable() {
        Log log = new Log();
        log.setExtraItem("k", "v");
        Map extras = log.getExtraItemMap();
        assertThrows(UnsupportedOperationException.class, () -> extras.put("new", "value"));
    }

    @Test
    void getExtraItemMapEmptyWhenNoExtras() {
        Log log = new Log();
        assertTrue(log.getExtraItemMap().isEmpty());
    }
}
