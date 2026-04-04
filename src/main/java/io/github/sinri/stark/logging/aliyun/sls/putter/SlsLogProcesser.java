package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.logging.aliyun.sls.putter.entity.LogGroup;
import io.github.sinri.stark.logging.base.Log;
import io.github.sinri.stark.logging.base.LogProcesser;
import io.github.sinri.stark.logging.base.impl.processor.StdoutLogProcesser;
import io.github.sinri.stark.logging.base.impl.render.JsonObjectLogRender;
import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SlsLogProcesser extends StarkVerticleBase implements LogProcesser {
    private final Map<String, Queue<Log>> queueMap = new ConcurrentHashMap<>();
    private final @Nullable AliyunSLSLogPutter aliyunSLSLogPutter;
    private final int bufferSize;
    private final SlsLogRender slsLogRender;
    private final String project;
    private final String logstore;
    private final AtomicBoolean toStopRef = new AtomicBoolean(false);
    private LogProcesser fallbackLogProcesser;

    public SlsLogProcesser(@Nullable AliyunSLSLogPutter aliyunSLSLogPutter, int bufferSize, String project, String logstore) {
        this.aliyunSLSLogPutter = aliyunSLSLogPutter;
        this.bufferSize = bufferSize;
        this.fallbackLogProcesser = new StdoutLogProcesser(new JsonObjectLogRender());
        this.project = project;
        this.logstore = logstore;
        this.slsLogRender = new SlsLogRender();
    }

    @Override
    public @Nullable Future<Void> process(String topic, Log log) {
        queueMap.computeIfAbsent(topic, k -> new ConcurrentLinkedQueue<>()).add(log);
        return null;
    }

    @Override
    protected Future<?> startVerticle() {
        getStark().asyncCallRepeatedly(repeatStopper -> oneFlushRound()
                          .eventually(() -> {
                              if (toStopRef.get()) {
                                  repeatStopper.stop();
                                  return Future.succeededFuture();
                              }
                              // check if any more logs to flush in queue
                              if (queueMap.values().stream()
                                          .anyMatch(queue -> !queue.isEmpty())) {
                                  return Future.succeededFuture();
                              } else {
                                  return getStark().asyncSleep(100L);
                              }
                          })
                  )
                  .eventually(() -> {
                      if (aliyunSLSLogPutter != null) {
                          return aliyunSLSLogPutter.close();
                      } else {
                          return Future.succeededFuture();
                      }
                  });
        return Future.succeededFuture();
    }

    private Future<Void> oneFlushRound() {
        List<String> topicList = queueMap.keySet().stream().toList();
        return getStark().asyncCallIteratively(topicList, topic -> {
            Queue<Log> logs = queueMap.get(topic);
            List<Log> logsToFlush = new ArrayList<>();
            while (!logs.isEmpty() && logsToFlush.size() < bufferSize) {
                logsToFlush.add(logs.poll());
            }
            return flushLogs(topic, logsToFlush);
        });
    }

    private Future<Void> flushLogs(String topic, List<Log> logsToFlush) {
        if (aliyunSLSLogPutter == null) {
            logsToFlush.forEach(log -> fallbackLogProcesser.process(topic, log));
            return Future.succeededFuture();
        } else {
            LogGroup logGroup = new LogGroup(topic, aliyunSLSLogPutter.getSource());
            logGroup.addLogItems(logsToFlush.stream().map(log -> slsLogRender.render(topic, log)).toList());
            return aliyunSLSLogPutter.putLogs(project, logstore, logGroup);
        }
    }

    public void stopProcessLogs() {
        toStopRef.set(true);
    }

    @Override
    protected Future<?> stopVerticle() {
        stopProcessLogs();
        return super.stopVerticle();
    }
}
