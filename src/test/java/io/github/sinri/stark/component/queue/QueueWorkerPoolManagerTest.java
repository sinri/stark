package io.github.sinri.stark.component.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueWorkerPoolManagerTest {

    @Test
    void unlimitedPoolIsNeverBusy() {
        QueueWorkerPoolManager m = new QueueWorkerPoolManager(0);
        m.whenOneWorkerStarts();
        m.whenOneWorkerStarts();
        assertFalse(m.isBusy());
    }

    @Test
    void busyWhenRunningReachesMax() {
        QueueWorkerPoolManager m = new QueueWorkerPoolManager(2);
        assertFalse(m.isBusy());
        m.whenOneWorkerStarts();
        assertFalse(m.isBusy());
        m.whenOneWorkerStarts();
        assertTrue(m.isBusy());
        m.whenOneWorkerEnds();
        assertFalse(m.isBusy());
    }

    @Test
    void changeMaxWorkerCountAffectsBusy() {
        QueueWorkerPoolManager m = new QueueWorkerPoolManager(1);
        m.whenOneWorkerStarts();
        assertTrue(m.isBusy());
        m.changeMaxWorkerCount(2);
        assertFalse(m.isBusy());
    }
}
