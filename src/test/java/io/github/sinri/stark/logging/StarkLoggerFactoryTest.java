package io.github.sinri.stark.logging;

import io.github.sinri.stark.logging.event.StarkLoggingEvent;
import io.github.sinri.stark.logging.format.StarkLogFormatter;
import io.github.sinri.stark.logging.sink.StarkLogSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.Logger;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StarkLoggerFactoryTest {

    @Test
    void resolvesMostSpecificLoggerLevelOverride() {
        StarkLoggerFactory factory = new StarkLoggerFactory(
                event -> event.getMessage(),
                Level.INFO,
                Map.of(
                        "io.github.sinri", Level.DEBUG,
                        "io.github.sinri.stark.logging", Level.TRACE
                ),
                new RecordingSink(),
                new StarkMDCAdapter()
        );

        Assertions.assertEquals(Level.TRACE, factory.getEffectiveLevel("io.github.sinri.stark.logging.LoggingAlphaTest"));
        Assertions.assertEquals(Level.DEBUG, factory.getEffectiveLevel("io.github.sinri.stark.core.Component"));
        Assertions.assertEquals(Level.INFO, factory.getEffectiveLevel("org.example.App"));
    }

    @Test
    void supportsSinkAbstractionAndPreservesRawMessageData() {
        RecordingSink sink = new RecordingSink();
        StarkLoggerFactory factory = new StarkLoggerFactory(
                event -> "rendered:" + event.getMessage(),
                Level.INFO,
                Map.of(),
                sink,
                new StarkMDCAdapter()
        );

        Logger logger = factory.getLogger("io.github.sinri.stark.logging.FactoryTest");
        logger.info("Hello {}", "World");

        Assertions.assertEquals(1, sink.events.size());
        Assertions.assertEquals("rendered:Hello World", sink.renderedEvents.getFirst());

        StarkLoggingEvent event = sink.events.getFirst();
        Assertions.assertEquals("Hello World", event.getMessage());
        Assertions.assertEquals("Hello {}", event.getMessagePattern());
        Assertions.assertArrayEquals(new Object[]{"World"}, event.getArguments());
    }

    @Test
    void snapshotsStructuredContextData() {
        Marker marker = MarkerFactory.getMarker("alpha");
        List<Marker> markers = new ArrayList<>();
        markers.add(marker);
        Map<String, String> mdc = new java.util.LinkedHashMap<>();
        mdc.put("traceId", "trace-1");
        List<KeyValuePair> kvps = new ArrayList<>();
        kvps.add(new KeyValuePair("a", "1"));

        StarkLoggingEvent event = new StarkLoggingEvent(
                "demo.logger",
                Level.INFO,
                "message",
                "message",
                null,
                null,
                1L,
                "main",
                markers,
                mdc,
                kvps
        );

        markers.add(MarkerFactory.getMarker("beta"));
        mdc.put("traceId", "trace-2");
        kvps.add(new KeyValuePair("z", "9"));

        Assertions.assertEquals(List.of(marker), event.getMarkers());
        Assertions.assertEquals(Map.of("traceId", "trace-1"), event.getMdcMap());
        Assertions.assertEquals(1, event.getKeyValuePairs().size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getMarkers().add(MarkerFactory.getMarker("gamma")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getMdcMap().put("x", "y"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getKeyValuePairs().add(new KeyValuePair("b", "2")));
    }

    @Test
    void rejectsNullConfigurationValues() {
        StarkLoggerFactory factory = new StarkLoggerFactory(
                event -> event.getMessage(),
                Level.INFO,
                Map.of(),
                new RecordingSink(),
                new StarkMDCAdapter()
        );

        Assertions.assertThrows(NullPointerException.class, () -> factory.setFormatter(null));
        Assertions.assertThrows(NullPointerException.class, () -> factory.setDefaultLevel(null));
        Assertions.assertThrows(NullPointerException.class, () -> factory.setSink(null));
        Assertions.assertThrows(NullPointerException.class, () -> factory.setLevel(null, Level.INFO));
        Assertions.assertThrows(NullPointerException.class, () -> factory.setLevel("demo", null));
    }

    private static final class RecordingSink implements StarkLogSink {
        private final List<StarkLoggingEvent> events = new ArrayList<>();
        private final List<String> renderedEvents = new ArrayList<>();

        @Override
        public void write(StarkLoggingEvent event, String renderedEvent) {
            events.add(event);
            renderedEvents.add(renderedEvent);
        }
    }
}
