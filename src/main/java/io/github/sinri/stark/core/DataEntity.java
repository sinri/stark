package io.github.sinri.stark.core;

import io.vertx.core.json.JsonObject;

public class DataEntity extends JsonObject {
    public DataEntity() {
        super();
    }

    public DataEntity(JsonObject jsonObject) {
        super(jsonObject.encode());
    }
}
