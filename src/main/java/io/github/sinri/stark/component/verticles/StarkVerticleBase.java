package io.github.sinri.stark.component.verticles;

import io.github.sinri.stark.core.LateObject;
import io.github.sinri.stark.core.Stark;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Stark 体系下的标准 Verticle 基类，实现接口 {@link StarkVerticle}，期望部署于 {@link Stark} 所实现的 {@link  Vertx} 实例下。
 *
 * @since 5.0.0
 */
@NullMarked
public abstract class StarkVerticleBase implements StarkVerticle {
    private final LateObject<Stark> lateStark = new LateObject<>();
    private final LateObject<Context> lateContext = new LateObject<>();
    private final Promise<Void> undeployPromise = Promise.promise();
    private volatile VerticleRunningStateEnum runningState;

    public StarkVerticleBase() {
        this.runningState = VerticleRunningStateEnum.BEFORE_RUNNING;
    }

    public static StarkVerticleBase wrap(
            Function<StarkVerticleBase, Future<?>> startFunc,
            Function<StarkVerticleBase, Future<?>> stopFunc
    ) {
        return new StarkVerticleBase() {
            @Override
            protected Future<?> startVerticle() {
                return startFunc.apply(this);
            }

            @Override
            protected Future<?> stopVerticle() {
                return stopFunc.apply(this);
            }
        };
    }

    public static StarkVerticleBase wrap(Function<StarkVerticleBase, Future<?>> startFunc) {
        return wrap(startFunc, Future::succeededFuture);
    }

    @Override
    public final Stark getStark() {
        return lateStark.get();
    }

    public ThreadingModel getCurrentThreadingModel() {
        return getContext().threadingModel();
    }

    public final JsonObject getVerticleInfo() {
        return new JsonObject()
                .put("identity", getVerticleIdentity())
                .put("class", this.getClass().getName())
                .put("config", this.config())
                .put("deployment_id", this.deploymentID())
                .put("thread_model", getCurrentThreadingModel().name());
    }

    public final VerticleRunningStateEnum getRunningState() {
        return runningState;
    }

    public final Future<String> deployMe(Vertx vertx, DeploymentOptions deploymentOptions) {
        Stark x;
        if (vertx instanceof Stark stark) {
            x = stark;
        } else {
            x = new Stark(vertx);
        }
        return deployMe(x, deploymentOptions);
    }

    /**
     * 在指定 Stark 实例下部署当前 Verticle，并使用指定的部署选项。
     *
     * @param stark             Stark 实例
     * @param deploymentOptions 部署选项
     * @return 一个异步结果；如果部署成功则返回部署 ID，如果部署失败则返回异常
     */
    public final Future<String> deployMe(Stark stark, DeploymentOptions deploymentOptions) {
        if (getRunningState() != VerticleRunningStateEnum.BEFORE_RUNNING) {
            return Future.failedFuture(new IllegalStateException("current verticle status is " + getRunningState()));
        }
        return stark.deployVerticle(this, deploymentOptions);
    }

    /**
     * 在当前已部署的 Verticle 所属的 Vertx 实例下解除部署。
     *
     * @return 一个异步完成，如果解除部署成功则返回，如果解除部署失败则返回异常
     */
    public final Future<Void> undeployMe() {
        String deploymentID = deploymentID();
        return getStark().undeploy(deploymentID);
    }

    /**
     * 获取当前 Verticle 实例的唯一标识或“身份”。
     *
     * @return 当前 Verticle 实例的身份，格式为 "标识@部署 ID"
     */
    public final String getVerticleInstanceIdentity() {
        return String.format("%s@%s", getVerticleIdentity(), deploymentID());
    }

    @Override
    public void init(Vertx vertx, Context context) {
        if (vertx instanceof Stark stark) {
            this.lateStark.initialize(stark);
        } else {
            this.lateStark.initialize(new Stark(vertx));
        }

        lateContext.initialize(context);
    }

    public Context getContext() {
        return lateContext.get();
    }

    @Override
    public String deploymentID() {
        return getContext().deploymentID();
    }


    @Override
    public @Nullable JsonObject config() {
        return getContext().config();
    }

    @Override
    public final Future<?> start() {
        runningState = VerticleRunningStateEnum.RUNNING;
        return Future.succeededFuture()
                     .compose(v -> startVerticle())
                     .onFailure(failure -> runningState = VerticleRunningStateEnum.DEPLOY_FAILED);
    }

    abstract protected Future<?> startVerticle();

    @Override
    public final Future<?> stop() {
        return stopVerticle()
                .onComplete(ar -> {
                    runningState = VerticleRunningStateEnum.AFTER_RUNNING;
                    if (ar.succeeded()) {
                        undeployPromise.tryComplete();
                    } else {
                        undeployPromise.tryFail(ar.cause());
                    }
                });
    }

    protected Future<?> stopVerticle() {
        return Future.succeededFuture();
    }

    @Override
    public final Future<Void> undeployed() {
        return undeployPromise.future();
    }

    @Override
    public final Future<?> deploy(Context context) {
        init(context.owner(), context);
        return start();
    }

    @Override
    public final Future<?> undeploy(Context context) {
        return stop();
    }
}
