package io.github.sinri.stark.logging.slf4j;

import io.github.sinri.stark.core.LateObject;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class StarkSlf4jServiceProvider implements SLF4JServiceProvider {
    public static final String REQUESTED_API_VERSION = "2.0.99";

    private final LateObject<ILoggerFactory> lateLoggerFactory = new LateObject<>();
//    private final LateObject<IMarkerFactory> lateMarkerFactory = new LateObject<>();
//    private final LateObject<MDCAdapter> lateMdcAdapter = new LateObject<>();
    private IMarkerFactory  markerFactory = new BasicMarkerFactory();
    private MDCAdapter mdcAdapter = new BasicMDCAdapter();

    @Override
    public ILoggerFactory getLoggerFactory() {
        return lateLoggerFactory.get();
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
//        return lateMarkerFactory.get();
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
//        return lateMdcAdapter.get();
        return  mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {
        lateLoggerFactory.initialize(new StarkSlf4jLoggerFactory());
//        lateMarkerFactory.initialize(new BasicMarkerFactory());
//        lateMdcAdapter.initialize(new BasicMDCAdapter());
    }
}

