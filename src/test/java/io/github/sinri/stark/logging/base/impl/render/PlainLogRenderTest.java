package io.github.sinri.stark.logging.base.impl.render;

import io.github.sinri.stark.logging.base.Log;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlainLogRenderTest {

    private final PlainLogRender render = PlainLogRender.INSTANCE;

    @Test
    void renderBasicLog() {
        Log log = new Log();
        log.setLevel("INFO");
        log.setMessage("basic message");

        String result = render.render("TestTopic", log);

        assertTrue(result.startsWith("* "), "should start with '* '");
        assertTrue(result.contains("[INFO]"), "should contain level");
        assertTrue(result.contains("TestTopic"), "should contain topic");
        assertTrue(result.contains("basic message"), "should contain message");
        assertTrue(result.contains("<"), "should contain thread in angle brackets");
        assertTrue(result.contains(">"), "should contain thread in angle brackets");
    }

    @Test
    void renderLogWithEmptyMessage() {
        Log log = new Log();
        log.setMessage("");

        String result = render.render("T", log);

        // 空消息不应单独出现在新行
        assertFalse(result.contains("\n"), "empty message should not produce newline body");
    }

    @Test
    void renderLogWithBlankMessage() {
        Log log = new Log();
        log.setMessage("   ");

        String result = render.render("T", log);

        // 纯空白消息也不应追加
        long newlineCount = result.chars().filter(c -> c == '\n').count();
        assertEquals(0, newlineCount, "blank message should not produce newline body");
    }

    @Test
    void renderLogWithContext() {
        Log log = new Log();
        log.setMessage("msg");
        log.setContext(new JsonObject().put("userId", 123).put("action", "login"));

        String result = render.render("T", log);

        assertTrue(result.contains("userId"), "should contain context key");
        assertTrue(result.contains("123"), "should contain context value");
        assertTrue(result.contains("action"), "should contain context key");
        assertTrue(result.contains("login"), "should contain context value");
    }

    @Test
    void renderLogWithThrowable() {
        Log log = new Log();
        log.setMessage("error occurred");
        log.setThrowable(new RuntimeException("test error"));

        String result = render.render("T", log);

        assertTrue(result.contains("RuntimeException"), "should contain exception class");
        assertTrue(result.contains("test error"), "should contain exception message");
    }

    @Test
    void renderLogWithExtras() {
        Log log = new Log();
        log.setMessage("msg");
        log.setExtraItem("requestId", "abc-123");

        String result = render.render("T", log);

        assertTrue(result.contains("Extra as following:"), "should contain extras header");
        assertTrue(result.contains("requestId"), "should contain extra key");
        assertTrue(result.contains("abc-123"), "should contain extra value");
    }

    @Test
    void renderLogNoContextNoThrowableNoExtras() {
        Log log = new Log();
        log.setMessage("simple");

        String result = render.render("T", log);

        assertFalse(result.contains("Extra as following:"));
    }

    @Test
    void renderClassificationJoinsWithComma() {
        String result = render.renderClassification(List.of("auth", "login", "retry"));
        assertEquals("auth,login,retry", result);
    }

    @Test
    void renderClassificationEmpty() {
        String result = render.renderClassification(List.of());
        assertEquals("", result);
    }

    @Test
    void renderContextFormatsEntries() {
        Map<String, Object> ctx = Map.of("k1", "v1", "k2", 42);
        String result = render.renderContext(ctx);

        assertTrue(result.contains("k1"), "should contain key");
        assertTrue(result.contains("v1"), "should contain value");
        assertTrue(result.contains("k2"));
        assertTrue(result.contains("42"));
    }

    @Test
    void singletonInstance() {
        assertSame(PlainLogRender.INSTANCE, PlainLogRender.INSTANCE);
    }
}
