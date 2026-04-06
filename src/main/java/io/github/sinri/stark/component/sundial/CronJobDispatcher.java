package io.github.sinri.stark.component.sundial;

import io.github.sinri.stark.component.sundial.cron.CronExpression;
import io.github.sinri.stark.component.sundial.cron.ParsedCalenderElements;
import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 日晷。
 * <p>
 * 单节点下的定时任务调度器。
 *
 * @since 5.0.0
 */
@NullMarked
public abstract class CronJobDispatcher extends StarkVerticleBase {

    private final Map<String, CronJobPlan> planMap = new ConcurrentHashMap<>();
    private final Logger logger;
    private @Nullable Long timerID;

    public CronJobDispatcher(Logger logger) {
        super();
        this.logger = logger;
    }

    public CronJobDispatcher() {
        this(LoggerFactory.universal().createLogger(CronJobDispatcher.class));
    }

    public final Logger getLogger() {
        return Objects.requireNonNull(logger);
    }

    @Override
    protected Future<Void> startVerticle() {
        return refreshPlans().compose(_ -> {
            int delaySeconds = 61 - CronExpression.parseCalenderToElements(Calendar.getInstance()).second;
            this.timerID = getStark().setPeriodic(delaySeconds * 1000L, 60_000L, timerID -> {
                handleEveryMinute(Calendar.getInstance());
                refreshPlans();
            });
            return Future.succeededFuture();
        });
    }

    private void handleEveryMinute(Calendar now) {
        ParsedCalenderElements parsedCalenderElements = new ParsedCalenderElements(now);
        planMap.forEach((key, plan) -> {
            if (plan.cronExpression().match(parsedCalenderElements)) {
                getLogger().debug(x -> x
                        .setMessage("Sundial Plan Matched")
                        .setContext(new JsonObject()
                                .put("plan_key", plan.key())
                                .put("plan_cron", plan.cronExpression().getRawCronExpression())
                                .put("now", parsedCalenderElements.toString())
                        )
                );

                plan.triggerVerticle(getStark(), now)
                    .onComplete(ar -> {
                        if (ar.failed()) {
                            getLogger().error(log -> log
                                    .setThrowable(ar.cause())
                                    .setMessage("Failed to deploy verticle for " + plan.key())
                            );
                        } else {
                            getLogger().info(log -> log
                                    .setMessage("Deployed verticle for " + plan.key() + " as " + ar.result())
                            );
                        }
                    });
            } else {
                getLogger().debug(x -> x
                        .setMessage("Sundial Plan Not Match")
                        .setContext(new JsonObject()
                                .put("plan_key", plan.key())
                                .put("plan_cron", plan.cronExpression().getRawCronExpression())
                                .put("now", parsedCalenderElements.toString())
                        )
                );
            }
        });
    }

    private Future<?> refreshPlans() {
        return getStark().sharedData().getLocalLock(
                "io.github.sinri.stark.component.sundial.Sundial.refreshPlans"
        ).compose(lock -> {
            return Future.succeededFuture().compose(v -> {
                return fetchPlans().compose(plans -> {
                    // treat null as NOT MODIFIED
                    if (plans != null) {
                        Set<String> toDelete = new HashSet<>(planMap.keySet());
                        plans.forEach(plan -> {
                            toDelete.remove(plan.key());
                            planMap.put(plan.key(), plan);
                        });
                        if (!toDelete.isEmpty()) {
                            toDelete.forEach(planMap::remove);
                        }
                    }
                    return Future.succeededFuture(null);
                });
            }).onComplete(ar -> {
                lock.release();
            });
        }).onFailure(throwable -> getLogger()
                .error(log -> log.setThrowable(throwable)
                                 .setMessage("Failed to refresh plans")));
    }

    /**
     * 异步获取最新定时任务计划集，根据结果进行全量覆盖或保持不动。
     *
     * @return 异步返回的定时任务计划集，用于覆盖更新当前的计划快照；如果异步返回了 null，则表示不更新计划快照。
     */
    abstract protected Future<@Nullable Collection<CronJobPlan>> fetchPlans();

    @Override
    protected Future<Void> stopVerticle() {
        if (this.timerID != null) {
            long x = timerID;
            getStark().cancelTimer(x);
        }
        return Future.succeededFuture();
    }

    /**
     * 以 WORKER 模式部署。
     *
     * @return 部署结果
     */
    public final Future<String> deployMe(Stark stark) {
        return super.deployMe(stark, new DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER));
    }
}
