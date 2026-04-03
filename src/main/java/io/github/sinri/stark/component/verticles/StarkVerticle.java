package io.github.sinri.stark.component.verticles;

import io.github.sinri.stark.core.Stark;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;

/**
 * Stark 体系下的标准 Verticle 可部署对象接口，期望部署于 {@link io.github.sinri.stark.core.Stark} 所实现的 {@link  Vertx} 实例下。
 * <p>
 * 通常，应尽可能使用 {@link StarkVerticleBase} 作为父类实现所需的类；除非唯一父类被占用。
 *
 * @since 5.0.0
 */
public interface StarkVerticle extends Deployable {
    /**
     * 初始化 Verticle。
     * <p>
     * 当 Verticle 实例被部署时，本方法由 Vert.x 调用；不要自行调用。
     *
     * @param vertx   部署所使用的 Vert.x 实例
     * @param context Verticle 所在的上下文
     */
    void init(Vertx vertx, Context context);

    /**
     * 获取 Verticle 部署的部署 ID。
     *
     * @return 部署 ID
     */
    String deploymentID();

    /**
     * 获取 Verticle 的配置。
     * <p>
     * 配置可在部署 Verticle 时指定。
     *
     * @return 配置
     */
    @Nullable JsonObject config();

    /**
     * 启动 Verticle。
     * <p>
     * 当 Verticle 实例被部署时，本方法由 Vert.x 调用；不要自行调用。
     * <p>
     * 如果启动过程包含耗时逻辑，可以重载本方法，并在启动完成时让返回的 {@link Future} 完成。
     *
     * @return 表示启动完成的 Future
     */
    Future<?> start() throws Exception;

    /**
     * 停止 Verticle。
     * <p>
     * 当 Verticle 实例被卸载时，本方法由 Vert.x 调用；不要自行调用。
     *
     * @return 表示清理完成的 Future
     */
    Future<?> stop() throws Exception;

    /**
     * 在部署后，可获取当前 Verticle 的信息，包括配置、部署 ID 等。
     *
     * @return 当前 Verticle 的信息
     */
    default JsonObject getVerticleInfo() {
        return new JsonObject()
                .put("identity", getVerticleIdentity())
                .put("class", this.getClass().getName())
                .put("config", this.config())
                .put("deployment_id", this.deploymentID())
                .put("thread_model", getCurrentThreadingModel().name());
    }

    /**
     * 获取当前 Verticle 类的唯一标识。
     *
     * @return 当前 Verticle 类的唯一标识
     */
    default String getVerticleIdentity() {
        return this.getClass().getName();
    }

    /**
     * 获取当前 Verticle 实例的唯一标识。
     *
     * @return 当前 Verticle 实例的身份
     */
    default String getVerticleInstanceIdentity() {
        return String.format("%s@%s", getVerticleIdentity(), deploymentID());
    }

    ThreadingModel getCurrentThreadingModel();

    Stark getStark();

    default Future<String> deployMe(Stark stark, DeploymentOptions deploymentOptions) {
        return stark.deployVerticle(this, deploymentOptions);
    }

    /**
     * 解除当前 Verticle 的部署。
     * <p>
     * 本方法不应在 {@link StarkVerticle#start()} 返回前调用。
     */
    default Future<Void> undeployMe() {
        String deploymentID = deploymentID();
        return getStark().undeploy(deploymentID);
    }

    /**
     * 获取当前 Verticle 的卸载 Future。
     * <p>
     * 在 stop 执行后（卸载完成后）返回。
     *
     * @return 当前 Verticle 的卸载 Future
     */
    Future<Void> undeployed();
}
