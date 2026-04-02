package io.github.sinri.stark.logging.base.impl;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ThrowableRenderTest {

    @Test
    void renderNullReturnsEmpty() {
        assertEquals("", ThrowableRender.renderThrowableChain(null));
    }

    @Test
    void renderSingleException() {
        RuntimeException ex = new RuntimeException("boom");
        String result = ThrowableRender.renderThrowableChain(ex, Collections.emptySet());

        assertTrue(result.contains("java.lang.RuntimeException: boom"), "should contain exception class and message");
        assertTrue(result.contains("ThrowableRenderTest"), "should contain test class in stack trace");
    }

    @Test
    void renderChainedExceptions() {
        Exception cause = new IllegalStateException("root cause");
        RuntimeException ex = new RuntimeException("wrapper", cause);

        String result = ThrowableRender.renderThrowableChain(ex, Collections.emptySet());

        assertTrue(result.contains("java.lang.RuntimeException: wrapper"));
        // cause 行用 "↑ " 前缀
        assertTrue(result.contains("↑ java.lang.IllegalStateException: root cause"));
    }

    @Test
    void renderTripleChain() {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        Exception c = new Exception("c", b);

        String result = ThrowableRender.renderThrowableChain(c, Collections.emptySet());

        assertTrue(result.contains("java.lang.Exception: c"));
        assertTrue(result.contains("↑ java.lang.Exception: b"));
        assertTrue(result.contains("↑ java.lang.Exception: a"));
    }

    @Test
    void ignorablePackageFolding() {
        RuntimeException ex = new RuntimeException("test");
        // 使用当前测试包作为可忽略前缀
        String testPackage = "io.github.sinri.stark.logging.base.impl.";
        Set<String> ignorable = Set.of(testPackage);

        String result = ThrowableRender.renderThrowableChain(ex, ignorable);

        // 折叠后应包含 [数量] 包名 格式
        assertTrue(result.contains(testPackage), "folded package prefix should appear");
        assertTrue(result.matches("(?s).*\\[\\d+].*"), "should contain [N] folding indicator");
    }

    @Test
    void noFoldingWhenNoMatchingPackage() {
        RuntimeException ex = new RuntimeException("test");
        Set<String> ignorable = Set.of("com.nonexistent.package.");

        String result = ThrowableRender.renderThrowableChain(ex, ignorable);

        assertFalse(result.contains("com.nonexistent.package."), "unmatched package should not appear");
        // 所有栈帧应该完整展示
        assertTrue(result.contains("ThrowableRenderTest"));
    }

    @Test
    void defaultIgnorableSetUsed() {
        RuntimeException ex = new RuntimeException("test");
        // 使用默认的忽略集合（不传第二个参数）
        String result = ThrowableRender.renderThrowableChain(ex);

        // 结果不应为空
        assertFalse(result.isEmpty());
        assertTrue(result.contains("java.lang.RuntimeException: test"));
    }
}
