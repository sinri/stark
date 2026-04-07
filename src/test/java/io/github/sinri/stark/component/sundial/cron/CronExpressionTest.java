package io.github.sinri.stark.component.sundial.cron;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronExpressionTest {

    @Test
    void starExpressionMatchesAnyCalendar() {
        CronExpression cron = new CronExpression("* * * * *");
        Calendar cal = new GregorianCalendar(2026, Calendar.APRIL, 7, 14, 30, 0);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertTrue(cron.match(cal));
    }

    @Test
    void specificMinuteMatches() {
        CronExpression cron = new CronExpression("7 * * * *");
        Calendar cal = new GregorianCalendar(2026, Calendar.APRIL, 7, 3, 7, 0);
        assertTrue(cron.match(cal));
        cal.set(Calendar.MINUTE, 8);
        assertFalse(cron.match(cal));
    }

    @Test
    void invalidFieldCountThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CronExpression("* * * *"));
    }

    @Test
    void invalidValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> new CronExpression("99 * * * *"));
    }
}
