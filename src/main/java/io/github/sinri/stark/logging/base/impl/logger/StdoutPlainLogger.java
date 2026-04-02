package io.github.sinri.stark.logging.base.impl.logger;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.impl.processor.PlainLogProcesser;

public final class StdoutPlainLogger implements Logger {
    private final String topic;

    public StdoutPlainLogger(String topic) {
        this.topic = topic;
    }

    @Override
    public String topic() {
        return topic;
    }

    @Override
    public void log(Log log) {
        PlainLogProcesser.INSTANCE.process(topic, log);
    }
}
