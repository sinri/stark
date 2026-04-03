package io.github.sinri.stark.logging.base;

import io.github.sinri.stark.core.DataEntity;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class Log extends DataEntity {
    public static final String TIMESTAMP = "timestamp";
    public static final String LEVEL = "level";
    public static final String MESSAGE = "message";
    public static final String THREAD = "thread";
    public static final String CONTEXT = "context";

    private @Nullable Throwable throwable;

    public Log() {
        super();
        this.put(TIMESTAMP, System.currentTimeMillis());
        this.put(LEVEL, "INFO");
        this.put(MESSAGE, "");
        // this.put("context", new JsonObject());
        this.put(THREAD, Thread.currentThread().toString());
    }

    public final long getTimestamp() {
        return getLong(TIMESTAMP);
    }

    public final String getMessage() {
        return getString(MESSAGE);
    }

    public final Log setMessage(String message) {
        put(MESSAGE, message);
        return this;
    }

    public final String getLevel() {
        return getString(LEVEL);
    }

    public final Log setLevel(String level) {
        put(LEVEL, level);
        return this;
    }

    public final Log setLevel(Level level) {
        return setLevel(level.name());
    }

    public final @Nullable JsonObject getContext() {
        return getJsonObject(CONTEXT);
    }

    public final Log setContext(@Nullable JsonObject context) {
        put(CONTEXT, context);
        return this;
    }

    public final @Nullable Throwable getThrowable() {
        return this.throwable;
    }

    public Log setThrowable(@Nullable Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    public final String getThread() {
        return getString(THREAD);
    }

    public final Log setExtraItem(String key, @Nullable Object value) {
        if (TIMESTAMP.equals(key) || LEVEL.equals(key) || MESSAGE.equals(key) || THREAD.equals(key) || CONTEXT.equals(key)) {
            throw new IllegalArgumentException("key is reserved");
        }
        put(key, value);
        return this;
    }

    public final Map<String, @Nullable Object> getExtraItemMap() {
        Map<String, @Nullable Object> extraMap = new HashMap<>();
        this.forEach(entry -> {
            if (!entry.getKey().equals(TIMESTAMP)
                    && !entry.getKey().equals(LEVEL)
                    && !entry.getKey().equals(MESSAGE)
                    && !entry.getKey().equals(CONTEXT)
                    && !entry.getKey().equals(THREAD)
            ) {
                extraMap.put(entry.getKey(), entry.getValue());
            }
        });
        return Collections.unmodifiableMap(extraMap);
    }
}
