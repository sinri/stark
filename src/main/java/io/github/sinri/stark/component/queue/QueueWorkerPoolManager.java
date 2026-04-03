package io.github.sinri.stark.component.queue;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 队列并发工作管理器
 *
 * @since 5.0.0
 */
@NullMarked
public class QueueWorkerPoolManager {
    private final AtomicInteger maxWorkerCountRef;
    private final AtomicInteger runningWorkerCounter;

    /**
     * @param maxWorkerCount 并发数量限制；为 0 表示不限制。
     */
    public QueueWorkerPoolManager(int maxWorkerCount) {
        this.maxWorkerCountRef = new AtomicInteger(maxWorkerCount);
        this.runningWorkerCounter = new AtomicInteger(0);
    }

    public QueueWorkerPoolManager() {
        this(0);
    }

    public void changeMaxWorkerCount(int maxWorkerCount) {
        this.maxWorkerCountRef.set(maxWorkerCount);
    }

    public boolean isBusy() {
        if (maxWorkerCountRef.get() <= 0) {
            return false;
        }
        return runningWorkerCounter.get() >= maxWorkerCountRef.get();
    }

    public void whenOneWorkerStarts() {
        this.runningWorkerCounter.incrementAndGet();
    }

    public void whenOneWorkerEnds() {
        this.runningWorkerCounter.decrementAndGet();
    }
}
