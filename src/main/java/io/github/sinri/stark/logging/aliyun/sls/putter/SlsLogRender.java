package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.logging.aliyun.sls.putter.entity.LogItem;
import io.github.sinri.stark.logging.base.JsonifiedThrowable;
import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogRender;
import io.vertx.core.json.JsonObject;

class SlsLogRender implements LogRender<LogItem> {
    @Override
    public LogItem render(String topic, Log log) {
        LogItem logItem = new LogItem(log.getTimestamp());
        logItem.addContent(Log.LEVEL, log.getLevel());
        logItem.addContent(Log.MESSAGE, log.getMessage());
        logItem.addContent(Log.THREAD, log.getThread());

        JsonObject context = log.getContext();
        if (context != null) {
            logItem.addContent(Log.CONTEXT, context.encode());
        }

        Throwable throwable = log.getThrowable();
        if (throwable != null) {
            logItem.addContent("exception", JsonifiedThrowable.wrap(throwable).encode());
        }

        log.getExtraItemMap().forEach((key, value) -> {
            if (value != null) {
                logItem.addContent(key, value.toString());
            }
        });

        return logItem;
    }
}
