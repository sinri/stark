package io.github.sinri.stark.core;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StarkTest {

    private Vertx vertx;
    private Stark stark;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        stark = new Stark(vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close().onComplete(ar -> latch.countDown());
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private void await(Future<?> future) throws Exception {
        future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private Throwable awaitFailure(Future<?> future) throws Exception {
        try {
            future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            fail("Expected failure");
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            return e.getCause();
        }
    }

    // --- Construction ---

    @Test
    void constructFromVertx() {
        assertNotNull(stark);
    }

    // --- asyncSleep ---

    @Test
    void asyncSleepCompletesAfterDelay() throws Exception {
        long before = System.currentTimeMillis();
        await(stark.asyncSleep(50));
        long elapsed = System.currentTimeMillis() - before;
        assertTrue(elapsed >= 40, "should wait at least ~50ms, was " + elapsed);
    }

    @Test
    void asyncSleepWithInvalidTimeUsesMinimum() throws Exception {
        await(stark.asyncSleep(0));
    }

    @Test
    void asyncSleepInterruptedEarly() throws Exception {
        Promise<Void> interrupter = Promise.promise();
        long before = System.currentTimeMillis();

        // Interrupt after 50ms
        stark.setTimer(50, id -> interrupter.complete());

        await(stark.asyncSleep(5000, interrupter));
        long elapsed = System.currentTimeMillis() - before;
        assertTrue(elapsed < 3000, "should be interrupted early, elapsed " + elapsed);
    }

    // --- asyncCallRepeatedly ---

    @Test
    void asyncCallRepeatedlyStopsWhenRequested() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        await(stark.asyncCallRepeatedly(stopper -> {
            int count = counter.incrementAndGet();
            if (count >= 5) {
                stopper.stop();
            }
            return Future.succeededFuture();
        }));

        assertEquals(5, counter.get());
    }

    @Test
    void asyncCallRepeatedlyFailsOnException() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        Throwable cause = awaitFailure(stark.asyncCallRepeatedly(stopper -> {
            if (counter.incrementAndGet() == 3) {
                return Future.failedFuture(new RuntimeException("boom"));
            }
            return Future.succeededFuture();
        }));

        assertEquals("boom", cause.getMessage());
        assertEquals(3, counter.get());
    }

    // --- asyncCallIteratively (single item) ---

    @Test
    void asyncCallIterativelySingleItem() throws Exception {
        List<String> source = List.of("a", "b", "c", "d");
        List<String> collected = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source.iterator(),
                item -> {
                    collected.add(item);
                    return Future.succeededFuture();
                }
        ));

        assertEquals(source, collected);
    }

    @Test
    void asyncCallIterativelySingleItemWithStopper() throws Exception {
        List<Integer> source = List.of(1, 2, 3, 4, 5);
        List<Integer> collected = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source.iterator(),
                (item, stopper) -> {
                    collected.add(item);
                    if (item == 3) {
                        stopper.stop();
                    }
                    return Future.succeededFuture();
                }
        ));

        assertEquals(List.of(1, 2, 3), collected);
    }

    // --- asyncCallIteratively (batch) ---

    @Test
    void asyncCallIterativelyBatch() throws Exception {
        List<Integer> source = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> batches = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source.iterator(),
                (List<Integer> batch) -> {
                    batches.add(new ArrayList<>(batch));
                    return Future.succeededFuture();
                },
                2
        ));

        assertEquals(3, batches.size());
        assertEquals(List.of(1, 2), batches.get(0));
        assertEquals(List.of(3, 4), batches.get(1));
        assertEquals(List.of(5), batches.get(2));
    }

    @Test
    void asyncCallIterativelyBatchWithStopper() throws Exception {
        List<Integer> source = List.of(1, 2, 3, 4, 5, 6);
        List<List<Integer>> batches = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source.iterator(),
                (List<Integer> batch, RepeatStopper stopper) -> {
                    batches.add(new ArrayList<>(batch));
                    if (batches.size() == 2) {
                        stopper.stop();
                    }
                    return Future.succeededFuture();
                },
                2
        ));

        assertEquals(2, batches.size());
        assertEquals(List.of(1, 2), batches.get(0));
        assertEquals(List.of(3, 4), batches.get(1));
    }

    @Test
    void asyncCallIterativelyBatchSizeValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                stark.asyncCallIteratively(
                        List.of(1).iterator(),
                        (List<Integer> batch) -> Future.succeededFuture(),
                        0
                )
        );
    }

    // --- asyncCallIteratively (Iterable overloads) ---

    @Test
    void asyncCallIterativelyIterable() throws Exception {
        List<String> source = List.of("x", "y", "z");
        List<String> collected = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source,
                item -> {
                    collected.add(item);
                    return Future.succeededFuture();
                }
        ));

        assertEquals(source, collected);
    }

    @Test
    void asyncCallIterativelyIterableWithStopper() throws Exception {
        List<String> source = List.of("a", "b", "c");
        List<String> collected = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source,
                (item, stopper) -> {
                    collected.add(item);
                    if (item.equals("b")) {
                        stopper.stop();
                    }
                    return Future.succeededFuture();
                }
        ));

        assertEquals(List.of("a", "b"), collected);
    }

    @Test
    void asyncCallIterativelyIterableBatch() throws Exception {
        List<Integer> source = List.of(10, 20, 30);
        List<List<Integer>> batches = new ArrayList<>();

        await(stark.asyncCallIteratively(
                source,
                (List<Integer> batch, RepeatStopper stopper) -> {
                    batches.add(new ArrayList<>(batch));
                    return Future.succeededFuture();
                },
                2
        ));

        assertEquals(2, batches.size());
        assertEquals(List.of(10, 20), batches.get(0));
        assertEquals(List.of(30), batches.get(1));
    }

    // --- asyncCallStepwise ---

    @Test
    void asyncCallStepwiseRange() throws Exception {
        List<Long> steps = new ArrayList<>();

        await(stark.asyncCallStepwise(0, 10, 3,
                (value, stopper) -> {
                    steps.add(value);
                    return Future.succeededFuture();
                }
        ));

        assertEquals(List.of(0L, 3L, 6L, 9L), steps);
    }

    @Test
    void asyncCallStepwiseEarlyStop() throws Exception {
        List<Long> steps = new ArrayList<>();

        await(stark.asyncCallStepwise(0, 100, 1,
                (value, stopper) -> {
                    steps.add(value);
                    if (value == 2) {
                        stopper.stop();
                    }
                    return Future.succeededFuture();
                }
        ));

        assertEquals(List.of(0L, 1L, 2L), steps);
    }

    @Test
    void asyncCallStepwiseTimes() throws Exception {
        List<Long> steps = new ArrayList<>();

        await(stark.asyncCallStepwise(3,
                (value, stopper) -> {
                    steps.add(value);
                    return Future.succeededFuture();
                }
        ));

        assertEquals(List.of(0L, 1L, 2L), steps);
    }

    @Test
    void asyncCallStepwiseTimesSimple() throws Exception {
        List<Long> steps = new ArrayList<>();

        await(stark.asyncCallStepwise(4, value -> {
            steps.add(value);
            return Future.succeededFuture();
        }));

        assertEquals(List.of(0L, 1L, 2L, 3L), steps);
    }

    @Test
    void asyncCallStepwiseZeroTimesReturnsImmediately() throws Exception {
        await(stark.asyncCallStepwise(0, value -> Future.succeededFuture()));
    }

    @Test
    void asyncCallStepwiseInvalidStepThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                stark.asyncCallStepwise(0, 10, 0, (v, s) -> Future.succeededFuture()));
    }

    @Test
    void asyncCallStepwiseInvalidRangeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                stark.asyncCallStepwise(10, 0, 1, (v, s) -> Future.succeededFuture()));
    }

    // --- asyncCallEndlessly ---

    @Test
    void asyncCallEndlesslyContinuesDespiteFailure() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        stark.asyncCallEndlessly(() -> {
            int c = counter.incrementAndGet();
            if (c % 2 == 0) {
                return Future.failedFuture(new RuntimeException("even fail"));
            }
            return Future.succeededFuture();
        });

        // Poll from test thread — the endless task occupies the event loop
        long deadline = System.currentTimeMillis() + 5000;
        while (counter.get() < 5 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(counter.get() >= 5, "should have run multiple times, got " + counter.get());
    }

    // --- Empty iteration ---

    @Test
    void asyncCallIterativelyEmptyIterator() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        await(stark.asyncCallIteratively(
                List.<String>of().iterator(),
                item -> {
                    counter.incrementAndGet();
                    return Future.succeededFuture();
                }
        ));

        assertEquals(0, counter.get());
    }
}
