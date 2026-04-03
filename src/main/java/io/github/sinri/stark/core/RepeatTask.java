package io.github.sinri.stark.core;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class RepeatTask {

    private final Function<RepeatStopper, Future<Void>> repeatTaskFunction;
    private final Promise<Void> finalPromise = Promise.promise();
    private final RepeatStopper repeatStopper = new RepeatStopper();
    private final AtomicBoolean executing = new AtomicBoolean(false);

    private RepeatTask(Function<RepeatStopper, Future<Void>> repeatTaskFunction) {
        this.repeatTaskFunction = repeatTaskFunction;
    }

    public static RepeatTask create(Function<RepeatStopper, Future<Void>> repeatTaskFunction) {
        return new RepeatTask(repeatTaskFunction);
    }

    public Future<Void> execute(Vertx vertx) {
        if (!executing.compareAndSet(false, true)) {
            return finalPromise.future();
        }
        run(vertx);
        return finalPromise.future();
    }

    private void run(Vertx vertx) {
        Future.succeededFuture()
              .compose(v -> {
                  if (repeatStopper.shouldStop()) {
                      return Future.succeededFuture();
                  }
                  return repeatTaskFunction.apply(repeatStopper);
              })
              .andThen(shouldStopAR -> {
                  if (shouldStopAR.succeeded()) {
                      if (repeatStopper.shouldStop()) {
                          finalPromise.complete();
                      } else {
                          vertx.setTimer(1L, x -> run(vertx));
                      }
                  } else {
                      finalPromise.fail(shouldStopAR.cause());
                  }
              });
    }

}
