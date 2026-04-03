package io.github.sinri.stark.component.sundial;

import io.github.sinri.stark.component.sundial.cron.StarkCronExpression;
import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;

import java.util.Calendar;

/**
 * 定时任务计划
 *
 * @since 5.0.0
 */
@NullMarked
public interface SundialPlan {
    static Future<Void> executeAndAwait(Stark stark, SundialPlan plan, Calendar now, Logger sundialSpecificLogger) {
        StarkVerticleBase starkVerticleBase = StarkVerticleBase.wrap(
                verticleBase -> {
                    verticleBase.getStark().asyncSleep(1)
                                .compose(v -> {
                                    return plan.execute(stark, now, sundialSpecificLogger);
                                })
                                .compose(v -> {
                                    return verticleBase.undeployMe();
                                });

                    return Future.succeededFuture();
                }
        );
        return starkVerticleBase.deployMe(
                                       stark,
                                       new DeploymentOptions()
                                               .setThreadingModel(plan.expectedThreadingModel())
                               )
                               .compose(deploymentId -> {
                                   return starkVerticleBase.undeployed();
                               });
    }

    static Future<Void> executeAndAwait(Stark stark, SundialPlan plan, Logger logger) {
        return executeAndAwait(stark, plan, Calendar.getInstance(), logger);
    }

    /**
     *
     * @return 定时任务计划名称
     */
    String key();

    /**
     *
     * @return 任务计划 Cron 表达式，精确到分钟
     */
    StarkCronExpression cronExpression();

    /**
     * 任务计划执行逻辑。
     *
     * @param stark                  定时任务运行的 Vertx 实例
     * @param now                   本次定时任务运行对应的触发时间
     * @param sundialSpecificLogger 日晷定时任务特定日志记录器
     */
    Future<Void> execute(Stark stark, Calendar now, Logger sundialSpecificLogger);

    default ThreadingModel expectedThreadingModel() {
        return ThreadingModel.WORKER;
    }
}
