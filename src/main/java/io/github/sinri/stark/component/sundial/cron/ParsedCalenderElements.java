package io.github.sinri.stark.component.sundial.cron;

import org.jspecify.annotations.NullMarked;

import java.util.Calendar;

/**
 * 日历类实例的解析结果，包括分钟、小时、天、月、星期、秒（可选）。
 *
 * @since 5.0.0
 */
@NullMarked
public class ParsedCalenderElements {
    public final int minute;
    public final int hour;
    public final int day;
    public final int month;
    public final int weekday;

    /**
     * 秒（可选）。
     * 默认为 0。
     */
    public final int second;

    /**
     * 使用指定的分钟、小时、天、月、星期和秒构造 ParsedCalenderElements 实例。
     * <p>此构造函数用于创建一个包含所有时间元素的解析结果对象。
     *
     * @param minute  分钟组件，取值范围 0-59
     * @param hour    小时组件，取值范围 0-23
     * @param day     月份中的天数，取值范围 1-31
     * @param month   月份，取值范围 1-12（1 表示一月）
     * @param weekday 星期几，取值范围 0-6（0 表示星期日）
     * @param second  秒组件，取值范围 0-59，通常用于调试
     */
    public ParsedCalenderElements(int minute, int hour, int day, int month, int weekday, int second) {
        this.minute = minute;
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.weekday = weekday;
        this.second = second;
    }

    /**
     * 使用指定的分钟、小时、天、月和星期构造 ParsedCalenderElements 实例。
     * <p>秒组件默认设置为 0。
     * <p>此构造函数是便捷方法，适用于不需要指定秒的场景。
     *
     * @param minute  分钟组件，取值范围 0-59
     * @param hour    小时组件，取值范围 0-23
     * @param day     月份中的天数，取值范围 1-31
     * @param month   月份，取值范围 1-12（1 表示一月）
     * @param weekday 星期几，取值范围 0-6（0 表示星期日）
     */
    public ParsedCalenderElements(int minute, int hour, int day, int month, int weekday) {
        this(minute, hour, day, month, weekday, 0);
    }

    /**
     * 从提供的 Calendar 对象构造 ParsedCalenderElements 实例。
     * <p>此构造函数从给定的 Calendar 对象中提取并设置分钟、小时、天、月、星期和秒组件。
     * <p>月份会被调整为基于 1 的格式（一月 = 1），星期会被调整为基于 0 的格式（星期日 = 0）。
     * <p>这是从 Calendar 对象创建解析结果的最便捷方式。
     *
     * @param currentCalendar 用于提取日期和时间组件的 Calendar 对象，不能为 null
     */
    public ParsedCalenderElements(Calendar currentCalendar) {
        minute = currentCalendar.get(Calendar.MINUTE);
        hour = currentCalendar.get(Calendar.HOUR_OF_DAY);
        day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        month = 1 + currentCalendar.get(Calendar.MONTH);// make JAN 1, ...
        weekday = currentCalendar.get(Calendar.DAY_OF_WEEK) - 1; // make sunday 0, ...
        second = currentCalendar.get(Calendar.SECOND);
    }

    /**
     * 返回解析后的日历元素的字符串表示。
     * <p>格式为 "(秒) 分钟 小时 天 月 星期"。
     * <p>例如："(30) 15 14 25 12 3" 表示 12 月 25 日 14:15:30，星期三。
     *
     * @return 格式为 "(秒) 分钟 小时 天 月 星期" 的字符串
     */
    @Override
    public String toString() {
        return "(" + second + ") " + minute + " " + hour + " " + day + " " + month + " " + weekday;
    }
}
