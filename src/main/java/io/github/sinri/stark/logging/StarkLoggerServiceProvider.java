package io.github.sinri.stark.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4J 2.x service provider for Stark Logging.
 * <p>
 * Configuration via system properties (fallback to environment variables):
 * <ul>
 *   <li>{@code stark.logging.format} / {@code STARK_LOGGING_FORMAT} — "text" (default) or "json"</li>
 *   <li>{@code stark.logging.level} / {@code STARK_LOGGING_LEVEL} — TRACE/DEBUG/INFO(default)/WARN/ERROR</li>
 *   <li>{@code stark.logging.formatter.class} / {@code STARK_LOGGING_FORMATTER_CLASS} — custom formatter FQCN</li>
 * </ul>
 */
public class StarkLoggerServiceProvider implements SLF4JServiceProvider {

    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    @Override
    public void initialize() {
        StarkMDCAdapter starkMDCAdapter = new StarkMDCAdapter();
        this.mdcAdapter = starkMDCAdapter;
        this.markerFactory = new BasicMarkerFactory();

        StarkLoggingConfiguration configuration = StarkLoggingConfiguration.load();
        configuration.getDiagnostics().forEach(System.err::println);
        this.loggerFactory = new StarkLoggerFactory(configuration, starkMDCAdapter);
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }
}
