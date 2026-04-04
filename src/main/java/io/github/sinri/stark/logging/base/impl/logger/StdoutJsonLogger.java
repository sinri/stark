package io.github.sinri.stark.logging.base.impl.logger;

import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;
import io.github.sinri.stark.logging.base.impl.render.JsonObjectLogRender;

public class StdoutJsonLogger extends StdoutLogger {
    private final LogProcesser logProcesser;

    public StdoutJsonLogger(String topic) {
        super(topic);
        logProcesser = new StdoutLogProcesser(new JsonObjectLogRender());
    }

    @Override
    public LogProcesser getLogProcesser() {
        return logProcesser;
    }
}
