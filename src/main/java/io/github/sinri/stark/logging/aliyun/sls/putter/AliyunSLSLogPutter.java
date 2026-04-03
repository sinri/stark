package io.github.sinri.stark.logging.aliyun.sls.putter;

import io.github.sinri.stark.logging.aliyun.sls.putter.entity.LogGroup;
import io.github.sinri.stark.logging.aliyun.sls.putter.protocol.Lz4Utils;
import io.github.sinri.stark.logging.base.Logger;
import io.github.sinri.stark.logging.base.impl.logger.StdoutPlainLogger;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * 阿里云日志服务上传 API 封装类。
 *
 * @since 5.0.0
 */
@NullMarked
public class AliyunSLSLogPutter implements Closeable {
    private final String accessKeyId;
    private final String accessKeySecret;
    private final WebClient webClient;
    private final String endpoint;
    private final Logger logger;

    public AliyunSLSLogPutter(Vertx vertx, String accessKeyId, String accessKeySecret, String endpoint) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.webClient = WebClient.create(vertx);
        this.endpoint = endpoint;
        this.logger = new StdoutPlainLogger(this.getClass().getName());
    }

    /**
     * Build source from configuration.
     * Source Expression should be:
     * - EMPTY/BLANK STRING or NULL: use SLS default source generation;
     * - A TEMPLATED STRING
     * --- Rule 1: Replace [IP] to local address;
     */

    public String buildSource(@Nullable String configuredSourceExpression) {
        if (configuredSourceExpression == null || configuredSourceExpression.isBlank()) {
            return "";
        }
        // Rule 1: Replace [IP] to local address
        String localHostAddress = getLocalHostAddress();
        if (localHostAddress == null) {
            logger.warn("Could not get local host address for SLS source!");
            return "";
        }
        return configuredSourceExpression.replaceAll("\\[IP]", localHostAddress);
    }

    @Override
    public void close(Completable<Void> completion) {
        logger.debug("Closing AliyunSLSLogPutter web client");
        this.webClient.close();
        completion.succeed();
    }

    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        close(promise);
        return promise.future();
    }

    public Future<Void> putLogs(String project, String logstore, LogGroup logGroup) {
        return putLogsImpl(project, logstore, logGroup);
    }

    /**
     * 调用PutLogs API。
     *
     * @param project  Project name
     * @param logstore Logstore name
     * @param logGroup LogGroup to be sent
     * @return Future of void if successful, or failed future with an error message
     */

    private Future<Void> putLogsImpl(String project, String logstore, LogGroup logGroup) {
        String uri = String.format("/logstores/%s/shards/lb", logstore);
        String url = String.format("https://%s.%s%s", project, endpoint, uri);

        String date = AliyunSlsSignatureKit.getGMTDate();
        String contentType = "application/x-protobuf";

        Map<String, String> headers = new HashMap<>();
        headers.put("Date", date);
        headers.put("Content-Type", contentType);
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("x-log-compresstype", "lz4");
        headers.put("Host", project + "." + endpoint);

        // Convert LogGroup to protobuf format
        Buffer raw = serializeLogGroup(logGroup);
        headers.put("x-log-bodyrawsize", String.valueOf(raw.length()));
        Buffer payload = Lz4Utils.compress(raw);
        headers.put("Content-Length", String.valueOf(payload.length()));

        try {
            var contentMd5 = Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("MD5").digest(payload.getBytes()));
            headers.put("Content-MD5", contentMd5);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }

        // Calculate signature and add authorization header
        String signature = AliyunSlsSignatureKit.calculateSignature(
                "POST",
                payload,
                contentType,
                date,
                headers,
                uri,
                null,
                accessKeySecret);
        headers.put("Authorization", "LOG " + accessKeyId + ":" + signature);

        HttpRequest<Buffer> request = this.webClient.postAbs(url);
        headers.forEach(request::putHeader);
        return request.sendBuffer(payload)
                      .compose(bufferHttpResponse -> {
                          if (bufferHttpResponse.statusCode() != 200) {
                              // System.out.println("write to sls: 200");
                              logger.error("put log failed [" + bufferHttpResponse.statusCode() + "] "
                                      + bufferHttpResponse.bodyAsString());
                          }
                          return Future.succeededFuture();
                      })
                      .recover(throwable -> {
                          logger.error(log -> log.setThrowable(throwable).setMessage("put log failed [X]"));
                          return Future.succeededFuture();
                      })
                      .mapEmpty();
    }

    /**
     * According to Aliyun SLS API documentation, we should send LogGroupList;
     * But let's try sending just the first LogGroup to see if that works.
     */

    private Buffer serializeLogGroup(LogGroup logGroup) {
        return Buffer.buffer(logGroup.toProtobuf().toByteArray());
    }

    private static @Nullable String getLocalHostAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
