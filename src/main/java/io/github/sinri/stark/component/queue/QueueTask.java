package io.github.sinri.stark.component.queue;

import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.LateObject;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;


/**
 * 队列任务类
 *
 * @since 5.0.0
 */
@NullMarked
public abstract class QueueTask extends StarkVerticleBase {
    private final LateObject<QueueWorkerPoolManager> lateQueueWorkerPoolManager = new LateObject<>();
    private final LateObject<Logger> lateQueueTaskLogger = new LateObject<>();

    public QueueTask() {
        super();
    }

    protected final QueueWorkerPoolManager getQueueWorkerPoolManager() {
        return lateQueueWorkerPoolManager.get();
    }

    final void setQueueWorkerPoolManager(QueueWorkerPoolManager queueWorkerPoolManager) {
        this.lateQueueWorkerPoolManager.initialize(queueWorkerPoolManager);
    }


    abstract public String getTaskReference();


    abstract public String getTaskCategory();

    protected abstract Logger buildQueueTaskLogger();


    protected final Logger getQueueTaskLogger() {
        return lateQueueTaskLogger.get();
    }

    /**
     * As of 4.1.3, if a worker thread is required, the threading model is set to WORKER;
     * otherwise, the threading model is set to VIRTUAL_THREAD if possible.
     */
    @Override
    protected Future<Void> startVerticle() {
        this.lateQueueTaskLogger.initialize(buildQueueTaskLogger());

        this.getQueueWorkerPoolManager().whenOneWorkerStarts();

        Future.succeededFuture()
              .compose(v -> {
                  notifyAfterDeployed();
                  return Future.succeededFuture();
              })
              .compose(v -> run())
              .recover(throwable -> {
                  getQueueTaskLogger().error(log -> log
                          .setThrowable(throwable)
                          .setMessage("QueueTask Caught throwable from Method run")
                  );
                  return Future.succeededFuture();
              })
              .eventually(() -> {
                  getQueueTaskLogger().info(r -> r.setMessage("QueueTask to undeploy"));
                  notifyBeforeUndeploy();
                  return undeployMe().onSuccess(done -> this.getQueueWorkerPoolManager().whenOneWorkerEnds());
              });

        return Future.succeededFuture();
    }


    abstract protected Future<Void> run();

    protected void notifyAfterDeployed() {
        getQueueTaskLogger().debug("QueueTask.notifyAfterDeployed");
    }

    protected void notifyBeforeUndeploy() {
        getQueueTaskLogger().debug("QueueTask.notifyBeforeUndeploy");
    }

    /**
     * @return 部署结果
     */

    public Future<String> deployMe(Stark stark) {
        var deploymentOptions = new DeploymentOptions();
        deploymentOptions.setThreadingModel(expectedThreadingModel());
        return super.deployMe(stark, deploymentOptions);
    }


    protected ThreadingModel expectedThreadingModel() {
        return ThreadingModel.WORKER;
    }

}
