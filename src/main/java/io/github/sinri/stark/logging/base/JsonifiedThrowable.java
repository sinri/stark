package io.github.sinri.stark.logging.base;

import io.github.sinri.stark.core.DataEntity;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class JsonifiedThrowable extends DataEntity {
    /**
     * 私有构造函数，用于内部创建实例。
     */
    private JsonifiedThrowable() {
        super();
    }

    /**
     * 将异常对象包装为 JSON 化的异常对象。
     * <p>
     * 使用默认的可忽略堆栈包集合和省略忽略堆栈选项。
     *
     * @param throwable 要包装的异常对象
     * @return JSON 化的异常对象
     */
    public static JsonifiedThrowable wrap(Throwable throwable) {
        return wrap(throwable, LoggingStackSpecification.IgnorableCallStackPackageSet, true);
    }

    /**
     * 将异常对象包装为 JSON 化的异常对象。
     * <p>
     * 此方法会递归处理异常链（cause），将整个异常链转换为 JSON 结构。
     * 堆栈跟踪信息会根据可忽略的包集合进行过滤和压缩。
     *
     * @param throwable                要包装的异常对象
     * @param ignorableStackPackageSet 可忽略的堆栈包集合，匹配这些包的堆栈项将被过滤
     * @param omitIgnoredStack         是否完全省略被忽略的堆栈项，如果为 false 则用摘要项替代
     * @return JSON 化的异常对象，包含异常链信息
     */
    public static JsonifiedThrowable wrap(
            Throwable throwable,
            Set<String> ignorableStackPackageSet,
            boolean omitIgnoredStack
    ) {
        JsonifiedThrowable x = new JsonifiedThrowable();
        x.put("class", throwable.getClass().getName());
        x.put("message", throwable.getMessage());
        x.put("stack", new JsonArray(filterStackTraceAndReduce(
                throwable.getStackTrace(),
                ignorableStackPackageSet,
                omitIgnoredStack)));
        x.put("cause", null);

        JsonifiedThrowable upper = x;
        Throwable cause = throwable.getCause();
        while (cause != null) {
            JsonifiedThrowable current = new JsonifiedThrowable();
            current.put("class", cause.getClass().getName());
            current.put("message", cause.getMessage());
            current.put("stack", new JsonArray(filterStackTraceAndReduce(cause.getStackTrace(), ignorableStackPackageSet,
                    omitIgnoredStack)));
            current.put("cause", null);
            upper.put("cause", current);
            upper = current;

            cause = cause.getCause();
        }
        return x;
    }

    /**
     * 过滤堆栈跟踪并压缩为堆栈项列表。
     * <p>
     * 根据可忽略的包集合过滤堆栈跟踪，将被忽略的连续堆栈项压缩为摘要项。
     *
     * @param stackTrace               堆栈跟踪元素数组
     * @param ignorableStackPackageSet 可忽略的堆栈包集合
     * @param omitIgnoredStack         是否完全省略被忽略的堆栈项
     * @return 过滤和压缩后的堆栈项列表
     */
    private static List<JsonifiedCallStackItem> filterStackTraceAndReduce(
            @Nullable StackTraceElement[] stackTrace,
            Set<String> ignorableStackPackageSet,
            boolean omitIgnoredStack
    ) {
        List<JsonifiedCallStackItem> items = new ArrayList<>();

        filterStackTrace(
                stackTrace,
                ignorableStackPackageSet,
                (ignoringClassPackage, ignoringCount) -> {
                    if (!omitIgnoredStack) {
                        items.add(new JsonifiedCallStackItem(ignoringClassPackage, ignoringCount));
                    }
                },
                stackTranceItem -> items.add(new JsonifiedCallStackItem(stackTranceItem))
        );

        return items;
    }

    /**
     * 过滤堆栈跟踪，将堆栈项分类处理。
     * <p>
     * 根据可忽略的包集合，将堆栈跟踪分为两类：
     * 1. 可忽略的堆栈项：连续的可忽略项会被压缩，通过 ignoredStackTraceItemsConsumer 处理
     * 2. 不可忽略的堆栈项：通过 stackTraceItemConsumer 处理
     *
     * @param stackTrace                     堆栈跟踪元素数组
     * @param ignorableStackPackageSet       可忽略的堆栈包集合
     * @param ignoredStackTraceItemsConsumer 处理被忽略的堆栈项摘要的回调（包名，数量）
     * @param stackTraceItemConsumer         处理不可忽略的堆栈项的回调
     */
    private static void filterStackTrace(
            @Nullable StackTraceElement @Nullable [] stackTrace,
            Set<String> ignorableStackPackageSet,
            BiConsumer<String, Integer> ignoredStackTraceItemsConsumer,
            Consumer<StackTraceElement> stackTraceItemConsumer
    ) {
        if (stackTrace != null) {
            String ignoringClassPackage = null;
            int ignoringCount = 0;
            for (StackTraceElement stackTranceItem : stackTrace) {
                if (stackTranceItem == null) continue;
                String className = stackTranceItem.getClassName();
                String matchedClassPackage = null;
                for (var cp : ignorableStackPackageSet) {
                    if (className.startsWith(cp)) {
                        matchedClassPackage = cp;
                        break;
                    }
                }
                if (matchedClassPackage == null) {
                    if (ignoringCount > 0) {
                        ignoredStackTraceItemsConsumer.accept(ignoringClassPackage, ignoringCount);
                        ignoringClassPackage = null;
                        ignoringCount = 0;
                    }

                    stackTraceItemConsumer.accept(stackTranceItem);
                } else {
                    if (ignoringCount > 0) {
                        if (Objects.equals(ignoringClassPackage, matchedClassPackage)) {
                            ignoringCount += 1;
                        } else {
                            ignoredStackTraceItemsConsumer.accept(ignoringClassPackage, ignoringCount);
                            ignoringClassPackage = matchedClassPackage;
                            ignoringCount = 1;
                        }
                    } else {
                        ignoringClassPackage = matchedClassPackage;
                        ignoringCount = 1;
                    }
                }
            }
            if (ignoringCount > 0) {
                ignoredStackTraceItemsConsumer.accept(ignoringClassPackage, ignoringCount);
            }
        }
    }

    /**
     * 获取异常类的全限定名。
     *
     * @return 异常类的全限定名，不会为 null
     */
    public String getThrowableClass() {
        return Objects.requireNonNull(getString("class"));
    }

    /**
     * 获取异常消息。
     *
     * @return 异常消息，如果异常没有消息则返回 null
     */
    public @Nullable String getThrowableMessage() {
        return getString("message");
    }

    /**
     * 获取异常的堆栈跟踪列表。
     * <p>
     * 返回的列表包含堆栈项，每个项可能是调用堆栈项或忽略堆栈摘要项。
     *
     * @return 堆栈跟踪项列表，不会为 null
     */
    public List<JsonifiedCallStackItem> getThrowableStack() {
        List<JsonifiedCallStackItem> items = new ArrayList<>();
        var a = getJsonArray("stack");
        if (a != null) {
            a.forEach(x -> {
                if (x instanceof JsonifiedCallStackItem) {
                    items.add((JsonifiedCallStackItem) x);
                } else if (x instanceof JsonObject) {
                    items.add(new JsonifiedCallStackItem((JsonObject) x));
                }
            });
        }
        return items;
    }

    /**
     * 获取导致此异常的原因异常。
     * <p>
     * 如果原因异常存在，返回其 JSON 化的异常对象；否则返回 null。
     *
     * @return 原因异常的 JSON 化对象，如果不存在则返回 null
     */
    @Nullable
    public JsonifiedThrowable getThrowableCause() {
        Object cause = getValue("cause");
        if (cause instanceof JsonifiedThrowable) {
            return (JsonifiedThrowable) cause;
        } else if (cause instanceof JsonObject jsonObject) {
            JsonifiedThrowable jsonifiedThrowable = new JsonifiedThrowable();

            jsonifiedThrowable.clear();
            jsonObject.forEach(entry->{
                jsonifiedThrowable.put(entry.getKey(), entry.getValue());
            });

            return jsonifiedThrowable;
        }
        return null;
    }

    /**
     * JSON 化的调用堆栈项。
     * <p>
     * 可以是实际的调用堆栈项，也可以是被忽略堆栈的摘要项。
     */
    public static class JsonifiedCallStackItem extends DataEntity {
        /**
         * 使用 JSON 对象构造堆栈项。
         *
         * @param jsonObject 包含堆栈项信息的 JSON 对象
         */
        private JsonifiedCallStackItem(JsonObject jsonObject) {
            super(jsonObject);
        }

        /**
         * 构造一个被忽略堆栈的摘要项。
         * <p>
         * 用于表示连续被忽略的堆栈项，包含包名和忽略数量。
         *
         * @param ignoringClassPackage 被忽略的类包名
         * @param ignoringCount        被忽略的堆栈项数量
         */
        private JsonifiedCallStackItem(String ignoringClassPackage, int ignoringCount) {
            super(new JsonObject()
                    .put("type", "ignored")
                    .put("package", ignoringClassPackage)
                    .put("count", ignoringCount));
        }

        /**
         * 从堆栈跟踪元素构造调用堆栈项。
         *
         * @param stackTranceItem 堆栈跟踪元素
         */
        private JsonifiedCallStackItem(StackTraceElement stackTranceItem) {
            super(new JsonObject()
                    .put("type", "call")
                    .put("class", stackTranceItem.getClassName())
                    .put("method", stackTranceItem.getMethodName())
                    .put("file", stackTranceItem.getFileName())
                    .put("line", stackTranceItem.getLineNumber()));
        }

        /**
         * 获取堆栈项类型。
         * <p>
         * 可能的值：
         * <ul>
         *   <li>"call" - 实际的调用堆栈项</li>
         *   <li>"ignored" - 被忽略堆栈的摘要项</li>
         * </ul>
         *
         * @return 堆栈项类型
         */
        public @Nullable String getType() {
            return getString("type");
        }

        /**
         * 获取被忽略的类包名。
         * <p>
         * 仅当类型为 "ignored" 时有效。
         *
         * @return 被忽略的类包名
         */
        public @Nullable String getPackage() {
            return getString("package");
        }

        /**
         * 获取被忽略的堆栈项数量。
         * <p>
         * 仅当类型为 "ignored" 时有效。
         *
         * @return 被忽略的堆栈项数量（字符串形式）
         */
        public @Nullable String getIgnoredStackCount() {
            return getString("count");
        }

        /**
         * 获取调用堆栈的类名。
         * <p>
         * 仅当类型为 "call" 时有效。
         *
         * @return 调用堆栈的类名
         */
        public @Nullable String getCallStackClass() {
            return getString("class");
        }

        /**
         * 获取调用堆栈的方法名。
         * <p>
         * 仅当类型为 "call" 时有效。
         *
         * @return 调用堆栈的方法名
         */
        public @Nullable String getCallStackMethod() {
            return getString("method");
        }

        /**
         * 获取调用堆栈的文件名。
         * <p>
         * 仅当类型为 "call" 时有效。
         *
         * @return 调用堆栈的文件名
         */
        public @Nullable String getCallStackFile() {
            return getString("file");
        }

        /**
         * 获取调用堆栈的行号。
         * <p>
         * 仅当类型为 "call" 时有效。
         *
         * @return 调用堆栈的行号（字符串形式）
         */
        public @Nullable String getCallStackLine() {
            return getString("line");
        }
    }
}
