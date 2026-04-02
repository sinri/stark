package io.github.sinri.stark.logging.base.impl.processor;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.impl.render.PlainLogRender;
import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

public final class PlainLogProcesser implements LogProcesser {
    public static final PlainLogProcesser INSTANCE = new PlainLogProcesser();

    private PlainLogProcesser() {
    }

    @Override
    public @Nullable Future<Void> process(String topic, Log log) {
        String rendered = PlainLogRender.INSTANCE.render(topic, log);
        System.out.println(rendered);
        return null;
    }
}
