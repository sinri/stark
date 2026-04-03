package io.github.sinri.stark.component.queue;

import io.github.sinri.stark.logging.base.Logger;
import io.vertx.core.Future;
import org.jspecify.annotations.NullMarked;

/**
 * 队列状态信号读取器
 *
 * @since 5.0.0
 */
@NullMarked
public interface QueueSignalReader {
    Logger getQueueManageLogger();

    Future<QueueSignal> readSignal();
}
