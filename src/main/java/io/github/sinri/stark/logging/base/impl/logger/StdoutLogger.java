package io.github.sinri.stark.logging.base.impl.logger;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.Logger;

public abstract class StdoutLogger implements Logger {
    private final String topic;
    public StdoutLogger(String topic) {
        this.topic = topic;
    }
    @Override
    public String topic() {
        return topic;
    }

    abstract protected LogProcesser getLogProcesser();
    @Override
    public void log(Log log) {
        getLogProcesser().process(topic, log);
    }
}
