package io.github.sinri.stark.logging.base;

import io.vertx.core.Handler;
import org.slf4j.event.Level;

public interface Logger {
    String topic();

    void log(Log log);

    default void log(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        log(log);
    }

    default void trace(Log log) {
        log.setLevel(Level.TRACE.name());
        log(log);
    }

    default void trace(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        trace(log);
    }

    default void trace(String message) {
        trace(log -> log.setMessage(message));
    }

    default void debug(Log log) {
        log.setLevel(Level.DEBUG.name());
        log(log);
    }

    default void debug(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        debug(log);
    }

    default void debug(String message) {
        debug(log -> log.setMessage(message));
    }

    default void info(Log log) {
        log.setLevel(Level.INFO.name());
        log(log);
    }

    default void info(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        info(log);
    }

    default void info(String message) {
        info(log -> log.setMessage(message));
    }

    default void warn(Log log) {
        log.setLevel(Level.WARN.name());
        log(log);
    }

    default void warn(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        warn(log);
    }

    default void warn(String message) {
        warn(log -> log.setMessage(message));
    }

    default void error(Log log) {
        log.setLevel(Level.ERROR.name());
        log(log);
    }

    default void error(Handler<Log> logHandler) {
        Log log = new Log();
        logHandler.handle(log);
        error(log);
    }

    default void error(String message) {
        error(log -> log.setMessage(message));
    }

}
