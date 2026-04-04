package io.github.sinri.stark.logging.slf4j;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serial;

public class StarkSlf4jLogger extends AbstractLogger {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Logger embeddedLogger;

    public StarkSlf4jLogger(String name) {
        this.name = name;
        this.embeddedLogger = LoggerFactory.universal().createLogger(name);
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    protected @Nullable String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(
            Level level,
            @Nullable Marker marker,
            String msg,
            @Nullable Object @Nullable [] args,
            @Nullable Throwable throwable) {
        String message;
        Throwable t = throwable;
        if (args != null && args.length > 0) {
            var tuple = MessageFormatter.arrayFormat(msg, args);
            message = tuple.getMessage();
            if (t == null) {
                t = tuple.getThrowable();
            }
        } else {
            message = msg;
        }
        Log log = new Log()
                .setLevel(level)
                .setMessage(message)
                .setThrowable(t);
        getEmbeddedLogger().log(log);
    }

    protected Logger getEmbeddedLogger() {
        return embeddedLogger;
    }
}
