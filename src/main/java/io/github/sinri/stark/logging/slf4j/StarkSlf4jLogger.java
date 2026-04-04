package io.github.sinri.stark.logging.slf4j;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LoggerFactory;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public class StarkSlf4jLogger extends MarkerIgnoringBase {
    public StarkSlf4jLogger(String name) {
        this.name = name;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        log(org.slf4j.event.Level.TRACE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log(org.slf4j.event.Level.TRACE, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(org.slf4j.event.Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(org.slf4j.event.Level.TRACE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(org.slf4j.event.Level.TRACE, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        log(org.slf4j.event.Level.DEBUG, msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log(org.slf4j.event.Level.DEBUG, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(org.slf4j.event.Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(org.slf4j.event.Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(org.slf4j.event.Level.DEBUG, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        log(org.slf4j.event.Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(org.slf4j.event.Level.INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(org.slf4j.event.Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(org.slf4j.event.Level.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(org.slf4j.event.Level.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        log(org.slf4j.event.Level.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(org.slf4j.event.Level.WARN, format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(org.slf4j.event.Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(org.slf4j.event.Level.WARN, format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(org.slf4j.event.Level.WARN, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        log(org.slf4j.event.Level.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(org.slf4j.event.Level.ERROR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(org.slf4j.event.Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(org.slf4j.event.Level.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(org.slf4j.event.Level.ERROR, msg, t);
    }

    private void log(org.slf4j.event.Level level, String message) {
        Log log = new Log()
                .setLevel(level)
                .setMessage(String.valueOf(message));
        targetLogger().log(log);
    }

    private void log(org.slf4j.event.Level level, String message, Throwable throwable) {
        Log log = new Log()
                .setLevel(level)
                .setMessage(String.valueOf(message))
                .setThrowable(throwable);
        targetLogger().log(log);
    }

    private void log(org.slf4j.event.Level level, String pattern, Object... arguments) {
        var tuple = MessageFormatter.arrayFormat(pattern, arguments);
        Log log = new Log()
                .setLevel(level)
                .setMessage(String.valueOf(tuple.getMessage()))
                .setThrowable(tuple.getThrowable());
        targetLogger().log(log);
    }

    private io.github.sinri.stark.logging.base.Logger targetLogger() {
        return LoggerFactory.universal().createLogger(name);
    }
}

