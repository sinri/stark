package io.github.sinri.stark.logging.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingStackSpecificationTest {

    @Test
    void containsExpectedVertxPrefixes() {
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("io.vertx.core."));
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("io.vertx.ext.web"));
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("io.vertx.mysqlclient"));
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("io.vertx.sqlclient"));
    }

    @Test
    void containsExpectedNettyPrefix() {
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("io.netty."));
    }

    @Test
    void containsExpectedJdkPrefixes() {
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("java.lang."));
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains("jdk.internal."));
    }

    @Test
    void setIsMutable() {
        String custom = "com.test.custom.";
        assertFalse(LoggingStackSpecification.IgnorableCallStackPackageSet.contains(custom));

        LoggingStackSpecification.IgnorableCallStackPackageSet.add(custom);
        assertTrue(LoggingStackSpecification.IgnorableCallStackPackageSet.contains(custom));

        // 清理
        LoggingStackSpecification.IgnorableCallStackPackageSet.remove(custom);
        assertFalse(LoggingStackSpecification.IgnorableCallStackPackageSet.contains(custom));
    }

    @Test
    void setIsNotEmpty() {
        assertFalse(LoggingStackSpecification.IgnorableCallStackPackageSet.isEmpty());
        assertEquals(7, LoggingStackSpecification.IgnorableCallStackPackageSet.size());
    }
}
