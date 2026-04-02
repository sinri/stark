package io.github.sinri.stark.logging.base;

import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.Set;

/**
 * Stack trace rendering specification.
 * <p>
 * Controls which package prefixes can be folded/ignored when rendering exception stack traces,
 * so that key information stands out.
 *
 * @since 5.0.0
 */
@NullMarked
public final class LoggingStackSpecification {
    /**
     * Set of package name prefixes that can be ignored when rendering exception stack traces.
     * <p>
     * Elements can be modified as needed.
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
