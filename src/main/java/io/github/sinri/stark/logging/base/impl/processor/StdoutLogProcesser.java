package io.github.sinri.stark.logging.base.impl.processor;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.LogRender;
import io.github.sinri.stark.logging.base.impl.render.PlainLogRender;
import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

public final class StdoutLogProcesser implements LogProcesser {
    private final LogRender<?> logRender;

    public StdoutLogProcesser() {
        this(new PlainLogRender());
    }

    public StdoutLogProcesser(LogRender<?> logRender) {
        this.logRender = logRender;
    }

    @Override
    public @Nullable Future<Void> process(String topic, Log log) {
        String rendered = logRender.renderToString(topic, log);
        System.out.println(rendered);
        return null;
    }
}
