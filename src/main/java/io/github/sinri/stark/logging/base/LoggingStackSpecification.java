package io.github.sinri.stark.logging.base;

import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.Set;

/**
 * 调用堆栈渲染规范。
 * <p>
 * 用于控制异常堆栈渲染时哪些包前缀可以被折叠/忽略，从而让关键信息更突出。
 *
 * @since 5.0.0
 */
@NullMarked
public final class LoggingStackSpecification {
    /**
     * 渲染异常调用堆栈时可忽略的包名前缀集合。
     * <p>
     * 可按需修改其中元素。
     */
    public static final Set<String> IgnorableCallStackPackageSet;

    static {
        IgnorableCallStackPackageSet = new HashSet<>(Set.of(
                "io.vertx.core.",
                "io.vertx.ext.web",
                "io.netty.",
                "java.lang.",
                "jdk.internal.",
                "io.vertx.mysqlclient",
                "io.vertx.sqlclient"
        ));
    }
}
