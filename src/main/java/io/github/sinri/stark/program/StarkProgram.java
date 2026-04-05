package io.github.sinri.stark.program;

import io.github.sinri.stark.component.queue.QueueDispatcher;
import io.github.sinri.stark.component.sundial.CronJobDispatcher;
import io.github.sinri.stark.component.web.HTTPService;
import io.github.sinri.stark.core.LateObject;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public abstract class StarkProgram {
    private final LateObject<Stark> lateStark = new LateObject<>();

    public void delegateMain(String[] args) {
        List<Future<Void>> serviceEndFutures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Logger programVerboseLogger = LoggerFactory.universal().createLogger(this.getClass());

        Future.succeededFuture()
              .compose(start -> {
                  programVerboseLogger.info("Delegate main, to parse args...");
                  return parseArgs(args);
              })
              .compose(_ -> {
                  programVerboseLogger.info("Parsed arguments, to build Stark...");
                  return buildStark()
                          .compose(stark -> {
                              this.lateStark.initialize(stark);
                              return Future.succeededFuture();
                          });
              })
              .compose(_ -> {
                  programVerboseLogger.info("Stark initialized, to Updated Universal Logger Factory.");
                  return buildLoggerFactory()
                          .compose(loggerFactory -> {
                              if (loggerFactory != null) {
                                  LoggerFactory.universal(loggerFactory);
                                  programVerboseLogger.info("Updated Universal Logger Factory.");
                              } else {
                                  programVerboseLogger.info("No Change to Universal Logger Factory");
                              }
                              return Future.succeededFuture();
                          });
              })
              .compose(_ -> {
                  programVerboseLogger.info("To build Queue Dispatcher...");
                  return buildQueueDispatcher()
                          .compose(queueDispatcher -> {
                              if (queueDispatcher == null) {
                                  programVerboseLogger.info("Queue dispatcher is not required.");
                                  return Future.succeededFuture();
                              } else {
                                  return queueDispatcher.deployMe(lateStark.get())
                                                        .compose(s -> {
                                                            programVerboseLogger.info("Queue dispatcher deployed: " + s);
                                                            serviceEndFutures.add(queueDispatcher.undeployed());
                                                            return Future.succeededFuture();
                                                        });
                              }
                          });
              })
              .compose(_ -> {
                  programVerboseLogger.info("To build CronJobDispatcher...");
                  return buildCronJobDispatcher()
                          .compose(cronJobDispatcher -> {
                              if (cronJobDispatcher == null) {
                                  programVerboseLogger.info("CronJobDispatcher is not required.");
                                  return Future.succeededFuture();
                              } else {
                                  return cronJobDispatcher.deployMe(lateStark.get())
                                                          .compose(s -> {
                                                              programVerboseLogger.info("CronJobDispatcher deployed: " + s);
                                                              serviceEndFutures.add(cronJobDispatcher.undeployed());
                                                              return Future.succeededFuture();
                                                          });
                              }
                          });
              })
              .compose(_ -> {
                  programVerboseLogger.info("To build HTTPService...");
                  return buildHTTPService()
                          .compose(httpService -> {
                              if (httpService == null) {
                                  programVerboseLogger.info("HTTPService is not required.");
                                  return Future.succeededFuture();
                              } else {
                                  return httpService.deployMe(lateStark.get())
                                                    .compose(s -> {
                                                        programVerboseLogger.info("HTTPService deployed: " + s);
                                                        serviceEndFutures.add(httpService.undeployed());
                                                        return Future.succeededFuture();
                                                    });
                              }
                          });
              })
              .onFailure(throwable -> {
                  // if any error occurred, stop the main process routine, i.e. to exit.
                  programVerboseLogger.error(log -> log.setMessage("Program failed to start").setThrowable(throwable));
                  latch.countDown();
              })
              .onSuccess(_ -> {
                  Future.all(serviceEndFutures)
                        .onComplete(ar -> {
                            // now all the verticles sent undeploy future, stop the main process routine, i.e. to exit.
                            programVerboseLogger.info("All services have been successfully undeployed.");
                            latch.countDown();
                        });
              })
        ;
    }

    abstract protected Future<Void> parseArgs(String[] args);

    abstract protected Future<Stark> buildStark();

    abstract protected Future<@Nullable LoggerFactory> buildLoggerFactory();

    abstract protected Future<@Nullable QueueDispatcher> buildQueueDispatcher();

    abstract protected Future<@Nullable CronJobDispatcher> buildCronJobDispatcher();

    abstract protected Future<@Nullable HTTPService> buildHTTPService();

}
