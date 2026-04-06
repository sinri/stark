package io.github.sinri.stark.component.queue;

import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.LateObject;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicReference;


/**
 * 队列服务实现。
 * <p>
 * 仅用于单节点模式。
 *
 * @since 5.0.0
 */
@NullMarked
public abstract class QueueDispatcher extends StarkVerticleBase
        implements NextQueueTaskSeeker, QueueSignalReader {

    private final Logger queueManageLogger;
    private final LateObject<QueueWorkerPoolManager> lateQueueWorkerPoolManager = new LateObject<>();
    private QueueStatus queueStatus = QueueStatus.INIT;
    private final AtomicReference<Long> nextRoutineTimerIdRef = new AtomicReference<>();

    public QueueDispatcher(Logger queueManageLogger) {
        super();
        this.queueManageLogger = queueManageLogger;
    }

    public QueueDispatcher() {
        this(LoggerFactory.universal().createLogger(QueueDispatcher.class));
    }

    @Override
    public Logger getQueueManageLogger() {
        return queueManageLogger;
    }

    public final QueueStatus getQueueStatus() {
        return queueStatus;
    }

    protected QueueDispatcher setQueueStatus(QueueStatus queueStatus) {
        if (this.queueStatus == QueueStatus.TERMINATED && queueStatus != QueueStatus.TERMINATED) {
            throw new IllegalStateException("Queue status TERMINATED cannot be reverted.");
        }
        this.queueStatus = queueStatus;
        return this;
    }

    /**
     * 创建队列并发工作管理器。
     * <p>
     * 默认实现为一个不限制并发的实例，可以重写本方法修改。
     */
    protected QueueWorkerPoolManager buildQueueWorkerPoolManager() {
        return new QueueWorkerPoolManager(0);
    }

    public QueueWorkerPoolManager getQueueWorkerPoolManager() {
        return lateQueueWorkerPoolManager.get();
    }

    /**
     * 队列运行前的清理整备逻辑。
     *
     */
    protected Future<Void> beforeQueueStart() {
        return Future.succeededFuture();
    }

    @Override
    protected Future<Void> startVerticle() {
        this.lateQueueWorkerPoolManager.initialize(buildQueueWorkerPoolManager());
        this.setQueueStatus(QueueStatus.RUNNING);
        return beforeQueueStart()
                .compose(v -> {
                    routine();
                    return Future.succeededFuture();
                });
    }

    private void routine() {
        if (getQueueStatus() == QueueStatus.TERMINATED) {
            this.getQueueManageLogger().debug(r -> r.setMessage("QueueDispatcher::routine skipped in TERMINATED state"));
            return;
        }
        this.getQueueManageLogger().debug(r -> r.setMessage("QueueDispatcher::routine start"));

        Future.succeededFuture()
              .compose(v -> this.readSignal())
              .recover(throwable -> {
                  this.getQueueManageLogger()
                      .debug(r -> r.setMessage("AS IS. Failed to read signal: " + throwable.getMessage()));
                  if (getQueueStatus() == QueueStatus.PAUSED || getQueueStatus() == QueueStatus.TERMINATED) {
                      return Future.succeededFuture(QueueSignal.STOP);
                  } else {
                      return Future.succeededFuture(QueueSignal.RUN);
                  }
              })
              .compose(signal -> {
                  if (signal == QueueSignal.STOP) {
                      return this.whenSignalStopCame();
                  } else if (signal == QueueSignal.RUN) {
                      return this.whenSignalRunCame();
                  } else {
                      return Future.failedFuture("Unknown Signal");
                  }
              })
              .eventually(() -> {
                  scheduleNextRoutine();
                  return Future.succeededFuture();
              })
        ;
    }

    private void scheduleNextRoutine() {
        if (getQueueStatus() == QueueStatus.TERMINATED) {
            this.getQueueManageLogger()
                .debug(r -> r.setMessage("Skip scheduling next routine in TERMINATED state"));
            return;
        }

        long waitingMs = this.getWaitingPeriodInMsWhenTaskFree();
        this.getQueueManageLogger()
            .debug(r -> r.setMessage("set timer for next routine after " + waitingMs + " ms"));

        long timerID = getStark().setTimer(waitingMs, x -> {
            nextRoutineTimerIdRef.compareAndSet(x, null);
            if (getQueueStatus() != QueueStatus.TERMINATED) {
                routine();
            }
        });
        Long old = nextRoutineTimerIdRef.getAndSet(timerID);
        if (old != null) {
            getStark().cancelTimer(old);
        }
    }

    private Future<Void> whenSignalStopCame() {
        if (getQueueStatus() == QueueStatus.RUNNING) {
            this.setQueueStatus(QueueStatus.PAUSED);
            this.getQueueManageLogger().info(r -> r.setMessage("Signal Stop Received"));
        }
        return Future.succeededFuture();
    }

    private Future<Void> whenSignalRunCame() {
        if (getQueueStatus() == QueueStatus.TERMINATED) {
            return Future.succeededFuture();
        }
        this.setQueueStatus(QueueStatus.RUNNING);

        return getStark().asyncCallRepeatedly(routineResult -> {
                             if (getQueueStatus() == QueueStatus.TERMINATED) {
                                 routineResult.stop();
                                 return Future.succeededFuture();
                             }
                             if (this.getQueueWorkerPoolManager().isBusy()) {
                                 return getStark().asyncSleep(1_000L);
                             }

                             return Future.succeededFuture()
                                          .compose(v -> this.seekNextTask())
                                          .compose(task -> {
                                              if (task == null) {
                                                  // 队列里已经空了，不必再找
                                                  this.getQueueManageLogger().debug(r -> r
                                                          .setMessage("No more task todo"));
                                                  // 通知 FutureUntil 结束
                                                  routineResult.stop();
                                                  return Future.succeededFuture();
                                              }

                                              // 队列里找出来一个task, deploy it (至于能不能跑起来有没有锁就不管了)
                                              this.getQueueManageLogger().info(r -> r
                                                      .setMessage("To run task: " + task.getTaskReference()));
                                              this.getQueueManageLogger().info(r -> r
                                                      .setMessage("Trusted that task  is already locked by seeker: " + task.getTaskReference()));

                                              task.setQueueWorkerPoolManager(this.getQueueWorkerPoolManager());

                                              return Future.succeededFuture()
                                                           .compose(v -> {
                                                               return task.deployMe(getStark());
                                                           })
                                                           .compose(
                                                                   deploymentID -> {
                                                                       this.getQueueManageLogger().info(r -> r.setMessage(
                                                                               "TASK [" + task.getTaskReference() + "] " +
                                                                                       "VERTICLE DEPLOYED: " + deploymentID));
                                                                       // 通知 FutureUntil 继续下一轮
                                                                       return Future.succeededFuture();
                                                                   },
                                                                   throwable -> {
                                                                       this.getQueueManageLogger().error(log -> log
                                                                               .setThrowable(throwable)
                                                                               .setMessage("CANNOT DEPLOY TASK [%s] VERTICLE".formatted(task.getTaskReference()))
                                                                       );
                                                                       // 通知 FutureUntil 继续下一轮
                                                                       return Future.succeededFuture();
                                                                   }
                                                           );
                                          });
                         })
                         .recover(throwable -> {
                             this.getQueueManageLogger().error(log -> log
                                     .setThrowable(throwable)
                                     .setMessage("QueueDispatcher 递归找活干里出现了奇怪的故障")
                             );
                             return Future.succeededFuture();
                         });
    }

    @Override
    protected Future<Void> stopVerticle() {
        this.setQueueStatus(QueueStatus.TERMINATED);
        Long timerId = nextRoutineTimerIdRef.getAndSet(null);
        if (timerId != null) {
            getStark().cancelTimer(timerId);
        }
        return Future.succeededFuture();
    }

    /**
     * 将本队列实例以 WORKER 线程模型部署。
     */
    public final Future<String> deployMe(Stark stark) {
        return super.deployMe(stark, new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
    }
}
