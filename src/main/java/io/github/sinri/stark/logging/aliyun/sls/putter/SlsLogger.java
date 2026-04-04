package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.Logger;

public class SlsLogger implements Logger {
    private final String topic;
    private final SlsLogProcesser logProcesser;

    public SlsLogger(String topic, SlsLogProcesser logProcesser) {
        this.topic = topic;
        this.logProcesser = logProcesser;
    }

    @Override
    public String topic() {
        return topic;
    }

    @Override
    public void log(Log log) {
        this.logProcesser.process(topic, log);
    }
}
