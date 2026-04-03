package io.github.sinri.stark.component.sundial.cron;


import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 分钟级 CRON 表达式。
 *
 * @since 5.0.0
 */
@NullMarked
public class StarkCronExpression {
    final Set<Integer> minuteOptions = new HashSet<>();
    final Set<Integer> hourOptions = new HashSet<>();
    final Set<Integer> dayOptions = new HashSet<>();
    final Set<Integer> monthOptions = new HashSet<>();
    final Set<Integer> weekdayOptions = new HashSet<>();
    private final String rawCronExpression;

    /**
     * 解析一个非空的分钟级 CRON 字符串，并构造本类的实例。
     * <p>
     * 分钟级 CRON 字符串的格式要求为：
     * {@code MINUTE HOUR DAY MONTH WEEKDAY}<br>
     * {@code MINUTE} 是分钟，范围是 0-59<br>
     * {@code HOUR} 是小时，范围是 0-23<br>
     * {@code DAY} 是天，范围是 1-31<br>
     * {@code MONTH} 是月，范围是 1-12<br>
     * {@code WEEKDAY} 是星期，范围是 0-6，其中 0 是星期日<br>
     * 各项中，除指定可选数字外，还可以使用{@code -}和{@code *}表示返回，
     * 使用{@code /}表示在范围内的步进。
     *
     * @param rawCronExpression cron
     * @throws RuntimeException if the provided cron expression is invalid (e.g., incorrect number of fields or invalid
     *                          values)
     * @see <a href="https://man7.org/linux/man-pages/man5/crontab.5.html">...</a>
     */
    public StarkCronExpression(String rawCronExpression) {
        this.rawCronExpression = rawCronExpression;

        String[] parts = rawCronExpression.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid Cron Expression");
        }

        String minuteExpression = parts[0]; // 0-59
        String hourExpression = parts[1]; // 0-23
        String dayExpression = parts[2]; // 1-31
        String monthExpression = parts[3]; // 1-12
        String weekdayExpression = parts[4];// 0-6

        parseField(minuteExpression, minuteOptions, 0, 59);
        parseField(hourExpression, hourOptions, 0, 23);
        parseField(dayExpression, dayOptions, 1, 31);
        parseField(monthExpression, monthOptions, 1, 12);
        parseField(weekdayExpression, weekdayOptions, 0, 6);
    }

    /**
     * 解析给定的 Calendar 对象并返回 ParsedCalenderElements 实例。
     * <p>
     * 该方法从 Calendar 对象中提取分钟、小时、日期、月份、星期和秒等时间组件，
     * 并将其封装为 ParsedCalenderElements 对象以便后续与 CRON 表达式进行匹配。
     *
     * @param currentCalendar 要解析的 Calendar 对象，不能为 null
     * @return 包含解析后的日期和时间组件的 ParsedCalenderElements 实例
     */
    public static ParsedCalenderElements parseCalenderToElements(Calendar currentCalendar) {
        return new ParsedCalenderElements(currentCalendar);
    }

    /**
     * 判断给定的 Calendar 对象是否匹配当前的 CRON 表达式。
     * <p>
     * 该方法会提取 Calendar 对象中的时间组件（分钟、小时、日期、月份、星期），
     * 并检查这些组件是否都符合 CRON 表达式中对应的规则。
     *
     * @param currentCalendar 要与 CRON 表达式进行匹配的 Calendar 对象，不能为 null
     * @return 如果 Calendar 对象匹配 CRON 表达式则返回 true，否则返回 false
     */
    public boolean match(Calendar currentCalendar) {
        ParsedCalenderElements parsedCalenderElements = new ParsedCalenderElements(currentCalendar);
        return match(parsedCalenderElements);
    }

    /**
     * 判断给定的已解析日历元素是否匹配当前的 CRON 表达式。
     * <p>
     * 该方法会检查 ParsedCalenderElements 中的各个时间组件（分钟、小时、日期、月份、星期）
     * 是否都在 CRON 表达式对应的允许值集合中。
     *
     * @param parsedCalenderElements 要与 CRON 表达式进行匹配的 ParsedCalenderElements 实例，不能为 null
     * @return 如果 ParsedCalenderElements 匹配 CRON 表达式则返回 true，否则返回 false
     */
    public boolean match(ParsedCalenderElements parsedCalenderElements) {
        return minuteOptions.contains(parsedCalenderElements.minute)
                && hourOptions.contains(parsedCalenderElements.hour)
                && dayOptions.contains(parsedCalenderElements.day)
                && monthOptions.contains(parsedCalenderElements.month)
                && weekdayOptions.contains(parsedCalenderElements.weekday);
    }

    /**
     * 解析 CRON 表达式的原始组件，并将有效值填充到选项集合中。
     * <p>
     * 该方法支持以下格式：
     * <ul>
     *   <li>{@code *} - 表示所有值（在指定范围内）</li>
     *   <li>单个数字 - 表示单个值，如 {@code 5}</li>
     *   <li>数字-数字 - 表示范围，如 {@code 1-5}</li>
     *   <li>星号/数字 - 表示从最小值开始按步进递增，如 {@code *&#47;2}</li>
     *   <li>范围/数字 - 表示在指定范围内按步进递增，如 {@code 1-10/2}</li>
     *   <li>值1,值2,值3 - 表示多个值或范围的列表，如 {@code 1,3,5} 或 {@code 1-3,5-7}</li>
     * </ul>
     *
     * @param rawComponent 要解析的 CRON 表达式的原始组件
     * @param optionSet    用于填充解析后值的集合
     * @param min          该组件允许的最小值
     * @param max          该组件允许的最大值
     * @throws IllegalArgumentException 如果原始组件无效或包含超出范围的值
     */
    private void parseField(String rawComponent, Set<Integer> optionSet, int min, int max) {
        if (rawComponent.equals("*")) {
            for (int i = min; i <= max; i++) {
                optionSet.add(i);
            }
            return;
        }

        ArrayList<String> parts = new ArrayList<>();
        if (rawComponent.contains(",")) {
            String[] t1 = rawComponent.split(",");
            parts.addAll(Arrays.asList(t1));
        } else {
            parts.add(rawComponent);
        }

        for (String part : parts) {
            part = part.trim();

            String[] split = part.split("/");
            int step;
            if (split.length == 2) {
                // range and step
                try {
                    step = Integer.parseInt(split[1]);
                    if (step <= 0) {
                        throw new IllegalArgumentException("Step value must be positive");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid step value: " + split[1], e);
                }
            } else if (split.length == 1) {
                // range
                step = 1;
            } else {
                throw new IllegalArgumentException("Invalid format: multiple '/' found");
            }
            String range = split[0];
            if (range.isEmpty()) {
                throw new IllegalArgumentException("Range cannot be empty");
            }

            Matcher matcher0 = Pattern.compile("^\\d+$").matcher(range);
            if (matcher0.matches()) {
                int value = Integer.parseInt(range);
                if (value < min || value > max) {
                    throw new IllegalArgumentException("Value " + value + " is out of range [" + min + ", " + max + "]");
                }
                if (step != 1) {
                    throw new IllegalArgumentException("Step is not allowed for single value: " + part);
                }
                optionSet.add(value);
                continue;
            }

            Matcher matcher1 = Pattern.compile("^(\\d+)-(\\d+)$").matcher(range);
            if (matcher1.matches()) {
                int start = Integer.parseInt(matcher1.group(1));
                int end = Integer.parseInt(matcher1.group(2));
                if (start < min || end > max || start > end) {
                    throw new IllegalArgumentException("Range [" + start + "-" + end + "] is invalid for [" + min + ", " + max + "]");
                }
                for (int i = start; i <= end; i += step) {
                    optionSet.add(i);
                }
                continue;
            }

            if (range.equals("*")) {
                for (int i = min; i <= max; i += step) {
                    optionSet.add(i);
                }
                continue;
            }

            throw new IllegalArgumentException("Invalid range format: " + range);
        }
    }

    /**
     * 返回用于初始化此 StarkCronExpression 实例的原始 CRON 表达式。
     *
     * @return 原始 CRON 表达式，非 null 字符串
     */
    public String getRawCronExpression() {
        return rawCronExpression;
    }

    /**
     * 返回此 StarkCronExpression 的字符串表示形式，即用于初始化实例的原始 CRON 表达式。
     *
     * @return 原始 CRON 表达式，非 null 字符串
     */
    @Override
    public String toString() {
        return getRawCronExpression();
    }
}
