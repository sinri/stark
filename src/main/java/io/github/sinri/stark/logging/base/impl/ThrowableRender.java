package io.github.sinri.stark.logging.base.impl;

import io.github.sinri.stark.logging.base.LoggingStackSpecification;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

/**
 * Utility class for rendering throwable cause chains.
 * <p>
 * Renders a throwable and its cause chain as readable text, with support for folding
 * stack trace entries from ignorable package prefixes.
 *
 * @since 5.0.0
 */
public class ThrowableRender {

    /**
     * Render a throwable cause chain using the default ignorable package set.
     *
     * @param throwable the throwable to render (may be null)
     * @return the rendered cause chain text; empty string if throwable is null
     */
    public static String renderThrowableChain(@Nullable Throwable throwable) {
        return renderThrowableChain(throwable, LoggingStackSpecification.IgnorableCallStackPackageSet);
    }

    /**
     * Render a throwable cause chain with a custom set of ignorable package prefixes.
     *
     * @param throwable                the throwable to render (may be null)
     * @param ignorableStackPackageSet package prefixes whose stack frames should be folded
     * @return the rendered cause chain text; empty string if throwable is null
     */
    public static String renderThrowableChain(@Nullable Throwable throwable, Set<String> ignorableStackPackageSet) {
        if (throwable == null) return "";
        Throwable cause = throwable.getCause();
        StringBuilder sb = new StringBuilder();
        sb
                .append("\t")
                .append(throwable.getClass().getName())
                .append(": ")
                .append(throwable.getMessage())
                .append(System.lineSeparator())
                .append(buildStackChainText(throwable.getStackTrace(), ignorableStackPackageSet));

        while (cause != null) {
            sb
                    .append("\t↑ ")
                    .append(cause.getClass().getName())
                    .append(": ")
                    .append(cause.getMessage())
                    .append(System.lineSeparator())
                    .append(buildStackChainText(cause.getStackTrace(), ignorableStackPackageSet))
            ;

            cause = cause.getCause();
        }

        return sb.toString();
    }

    private static String buildStackChainText(@Nullable StackTraceElement @Nullable [] stackTrace, Set<String> ignorableStackPackageSet) {
        StringBuilder sb = new StringBuilder();
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
                        sb.append("\t\t")
                          .append("[").append(ignoringCount).append("] ")
                          .append(ignoringClassPackage)
                          .append(System.lineSeparator());

                        ignoringClassPackage = null;
                        ignoringCount = 0;
                    }

                    sb.append("\t\t")
                      .append(stackTranceItem.getClassName())
                      .append(".")
                      .append(stackTranceItem.getMethodName())
                      .append(" (")
                      .append(stackTranceItem.getFileName())
                      .append(":")
                      .append(stackTranceItem.getLineNumber())
                      .append(")")
                      .append(System.lineSeparator());
                } else {
                    if (ignoringCount > 0) {
                        if (Objects.equals(ignoringClassPackage, matchedClassPackage)) {
                            ignoringCount += 1;
                        } else {
                            sb.append("\t\t")
                              .append("[").append(ignoringCount).append("] ")
                              .append(ignoringClassPackage)
                              .append(System.lineSeparator());

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
                sb.append("\t\t")
                  .append("[").append(ignoringCount).append("] ")
                  .append(ignoringClassPackage)
                  .append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

}
