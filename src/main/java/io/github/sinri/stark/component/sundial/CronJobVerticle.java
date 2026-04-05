package io.github.sinri.stark.component.sundial;

import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.LoggerFactory;

import java.util.Calendar;

public abstract class CronJobVerticle extends StarkVerticleBase {
    private final Calendar triggerTime;
    private final Logger logger;

    public CronJobVerticle(Calendar triggerTime) {
        this.triggerTime = triggerTime;
        this.logger = LoggerFactory.universal().createLogger(CronJobVerticle.class);
    }

    public final Calendar getTriggerTime() {
        return triggerTime;
    }

    public  final Logger getLogger() {
        return logger;
    }
}
