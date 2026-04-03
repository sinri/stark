package io.github.sinri.stark.core;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

interface StartAsyncMixin extends Vertx {
    /**
     * 非阻塞地{@code 睡眠}一段时间。
     * <p>
     * 注意，这并不是真正意义上的线程睡眠，而是通过定时回调机制实现的，因此并不会阻塞线程，也不会阻止进程退出。
     *
     * @param time 以毫秒计的时间，有效值最小为 1 毫秒；如果值无效会强制置为 1毫秒。
     * @return 一个{@link Future}，表示设定时间已到
     */
    default Future<Void> asyncSleep(long time) {
        return asyncSleep(time, null);
    }

    /**
     * 非阻塞地{@code 睡眠}一段时间，并允许（提前）主动中断。
     *
     * @param time        以毫秒计的时间，有效值最小为 1 毫秒；如果值无效会强制置为 1毫秒。
     * @param interrupter 一个可选的{@link Promise}，供异步中断
     * @return 一个{@link Future}，表示设定时间已到，或设置的中断被触发。
     */
    default Future<Void> asyncSleep(long time, @Nullable Promise<Void> interrupter) {
        Promise<Void> promise = Promise.promise();
        time = Math.max(1, time);
        long timer_id = setTimer(time, timerID -> {
            promise.tryComplete();
        });
        if (interrupter != null) {
            interrupter.future().onSuccess(interrupted -> {
                cancelTimer(timer_id);
                promise.tryComplete();
            });
        }
        return promise.future();
    }

    /**
     * Repeatedly execute an async task until the {@link RepeatStopper} is triggered or the task fails.
     *
     * @param processor the async loop body that receives a {@link RepeatStopper} to signal completion
     * @return async result that completes when the loop stops
     */
    default Future<Void> asyncCallRepeatedly(Function<RepeatStopper, Future<Void>> processor) {
        return RepeatTask.create(processor).execute(this);
    }

    /**
     * 针对一个迭代器，基于异步循环调用，进行异步批量迭代执行，并可以按需在迭代执行方法体里提前中断任务。
     *
     * @param <T>            迭代器内的迭代对象类型
     * @param iterator       迭代器
     * @param itemsProcessor 批量迭代执行逻辑
     * @param batchSize      批量执行量
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterator<T> iterator,
            BiFunction<List<T>, RepeatStopper, Future<Void>> itemsProcessor,
            int batchSize
    ) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be greater than 0");

        return asyncCallRepeatedly(repeatedlyCallTask -> {
            List<T> buffer = new ArrayList<>();

            while (buffer.size() < batchSize) {
                if (iterator.hasNext()) {
                    buffer.add(iterator.next());
                } else {
                    break;
                }
            }

            if (buffer.isEmpty()) {
                repeatedlyCallTask.stop();
                return Future.succeededFuture();
            }

            return itemsProcessor.apply(buffer, repeatedlyCallTask);
        });
    }

    /**
     * 针对一个迭代器，基于异步循环调用，进行异步批量迭代执行。
     *
     * @param iterator       迭代器
     * @param itemsProcessor 批量迭代执行逻辑
     * @param batchSize      批量执行量
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterator<T> iterator,
            Function<List<T>, Future<Void>> itemsProcessor,
            int batchSize
    ) {
        return asyncCallIteratively(
                iterator,
                (ts, repeatedlyCallTask) -> itemsProcessor.apply(ts),
                batchSize);
    }

    /**
     * 针对一个可迭代物，基于异步循环调用，进行异步批量迭代执行。
     *
     * @param <T>            可迭代物的迭代对象的类型
     * @param iterable       可迭代物
     * @param itemsProcessor 批量迭代执行逻辑
     * @param batchSize      批量执行量
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterable<T> iterable,
            BiFunction<List<T>, RepeatStopper, Future<Void>> itemsProcessor,
            int batchSize
    ) {
        return asyncCallIteratively(iterable.iterator(), itemsProcessor, batchSize);
    }

    /**
     * 针对一个迭代器，基于异步循环调用，进行异步迭代执行，并可以按需在迭代执行方法体里提前中断任务。
     *
     * @param <T>           迭代器内的迭代对象类型
     * @param iterator      迭代器
     * @param itemProcessor 迭代执行逻辑
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterator<T> iterator,
            BiFunction<T, RepeatStopper, Future<Void>> itemProcessor
    ) {
        return asyncCallRepeatedly(routineResult -> Future
                .succeededFuture()
                .compose(v -> {
                    if (iterator.hasNext()) {
                        return itemProcessor.apply(iterator.next(), routineResult);
                    } else {
                        routineResult.stop();
                        return Future.succeededFuture();
                    }
                }));
    }

    /**
     * 针对一个迭代器，基于异步循环调用，进行异步迭代执行。
     *
     * @param <T>           迭代器内的迭代对象类型
     * @param iterator      迭代器
     * @param itemProcessor 迭代执行逻辑
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterator<T> iterator,
            Function<T, Future<Void>> itemProcessor) {
        return asyncCallIteratively(
                iterator,
                (t, repeatedlyCallTask) -> itemProcessor.apply(t)
        );
    }

    /**
     * 针对一个可迭代物，基于异步循环调用，进行异步迭代执行。
     *
     * @param iterable      可迭代物
     * @param itemProcessor 迭代执行逻辑
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterable<T> iterable,
            Function<T, Future<Void>> itemProcessor) {
        return asyncCallIteratively(iterable.iterator(), itemProcessor);
    }

    /**
     * 针对一个可迭代物，基于异步循环调用，进行异步迭代执行，并可以按需在迭代执行方法体里提前中断任务
     *
     * @param <T>           迭代器内的迭代对象类型
     * @param iterable      可迭代物
     * @param itemProcessor 迭代执行逻辑
     * @return 异步循环执行结果
     */
    default <T extends @Nullable Object> Future<Void> asyncCallIteratively(
            Iterable<T> iterable,
            BiFunction<T, RepeatStopper, Future<Void>> itemProcessor) {
        return asyncCallIteratively(iterable.iterator(), itemProcessor);
    }

    /**
     * 基于给定的起始、终止、步长数值，基于异步循环调用，进行异步步进循环。
     * <p>
     * 步进方向要求是增量且可达的；因此，如果起始数值大于终止数值，或步进数值小于等于 0，将抛出异常。
     *
     * @param start     起始数值。
     * @param end       终止数值
     * @param step      步长数值
     * @param processor 异步步进循环逻辑
     * @return 异步循环执行结果
     * @throws IllegalArgumentException 当步进不满足增量且可达时抛出
     */
    default Future<Void> asyncCallStepwise(
            long start, long end, long step,
            BiFunction<Long, RepeatStopper, Future<Void>> processor) {
        if (step <= 0)
            throw new IllegalArgumentException("step must be greater than 0");
        if (start > end)
            throw new IllegalArgumentException("start must not be greater than end");
        AtomicLong ptr = new AtomicLong(start);
        return asyncCallRepeatedly(task -> Future
                .succeededFuture()
                .compose(vv -> processor.apply(ptr.get(), task)
                                        .compose(v -> {
                                            long y = ptr.addAndGet(step);
                                            if (y >= end) {
                                                task.stop();
                                            }
                                            return Future.succeededFuture();
                                        })));
    }

    /**
     * 基于异步循环调用，进行异步的指定次数步进循环，可以提前中断。
     * <p>
     * Based on {@link #asyncCallStepwise(long, long, long, BiFunction)} with start=0 and step=1.
     *
     * @param times     循环次数。即终止数值。当循环次数小于等于 0 时，将直接返回成功结果。
     * @param processor 异步步进循环逻辑
     * @return 异步循环执行结果
     */
    default Future<Void> asyncCallStepwise(
            long times,
            BiFunction<Long, RepeatStopper, Future<Void>> processor
    ) {
        if (times <= 0) {
            return Future.succeededFuture();
        }
        return asyncCallStepwise(0, times, 1, processor);
    }

    /**
     * 基于异步循环调用，进行异步的指定次数步进循环。
     *
     * @param times     循环次数。即终止数值。当循环次数小于等于 0 时，将直接返回成功结果。
     * @param processor 异步步进循环逻辑
     * @return 异步循环执行结果
     */
    default Future<Void> asyncCallStepwise(
            long times,
            Function<Long, Future<Void>> processor
    ) {
        if (times <= 0) {
            return Future.succeededFuture();
        }
        return asyncCallStepwise(0, times, 1, (aLong, repeatedlyCallTask) -> processor.apply(aLong));
    }

    /**
     * 无限循环执行一个异步逻辑，即时循环体抛出异常也不停止。
     * <p>
     * 使用本方法之前，应确保该逻辑符合要求且没有副作用。
     *
     * @param supplier 异步循环逻辑
     */
    default void asyncCallEndlessly(Supplier<Future<Void>> supplier) {
        asyncCallRepeatedly(routineResult -> Future
                .succeededFuture()
                .compose(v -> supplier.get())
                .recover(err -> Future.succeededFuture()));
    }
}
