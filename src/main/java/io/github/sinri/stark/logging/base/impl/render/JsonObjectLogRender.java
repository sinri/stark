package io.github.sinri.stark.logging.base.impl.render;

import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogRender;
import io.vertx.core.json.JsonObject;

public class JsonObjectLogRender implements LogRender<JsonObject> {
    @Override
    public JsonObject render(String topic, Log log) {
        return log;
    }
}
