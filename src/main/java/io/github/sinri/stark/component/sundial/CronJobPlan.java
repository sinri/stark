package io.github.sinri.stark.component.sundial;

import io.github.sinri.stark.component.sundial.cron.CronExpression;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;

/**
 * 定时任务计划
 *
 * @since 5.0.0
 */
@NullMarked
public interface CronJobPlan {

    /**
     *
     * @return 定时任务计划名称
     */
    String key();

    /**
     *
     * @return 任务计划 Cron 表达式，精确到分钟
     */
    CronExpression cronExpression();

    default ThreadingModel expectedThreadingModel() {
        return ThreadingModel.WORKER;
    }

    Class<? extends CronJobVerticle> triggerVerticleClass();

    default Future<String> triggerVerticle(Stark stark, Calendar triggerTime) {
        CronJobVerticle cronJobVerticle;
        try {
            cronJobVerticle = triggerVerticleClass()
                    .getConstructor(Calendar.class)
                    .newInstance(triggerTime);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            return Future.failedFuture(e);
        }
        return cronJobVerticle.deployMe(stark, new DeploymentOptions().setThreadingModel(expectedThreadingModel()));
    }
}
