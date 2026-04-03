package io.github.sinri.stark.core;

import io.vertx.core.Vertx;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.VertxWrapper;

public class Stark extends VertxWrapper implements StartAsyncMixin {
    public Stark(Vertx vertx) {
        this((VertxInternal) vertx);
    }

    public Stark(VertxInternal delegate) {
        super(delegate);
    }
}
