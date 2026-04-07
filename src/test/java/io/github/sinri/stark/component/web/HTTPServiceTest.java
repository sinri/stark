package io.github.sinri.stark.component.web;

import io.github.sinri.stark.core.Stark;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HTTPServiceTest {

    private Vertx vertx;
    private Stark stark;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        stark = new Stark(vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private static final class FixedPortHttpService extends HTTPService {
        private final int port;

        FixedPortHttpService(int port) {
            this.port = port;
        }

        @Override
        protected HttpServerOptions buildHttpServerOptions() {
            return new HttpServerOptions().setPort(port).setHost("127.0.0.1");
        }

        @Override
        protected void configureRoutes(Router router) {
            router.get("/ping").handler(ctx -> ctx.response().end("pong"));
        }
    }

    @Test
    void listenServesConfiguredRoute() throws Exception {
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }
        FixedPortHttpService service = new FixedPortHttpService(port);
        service.deployMe(stark).toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        WebClient client = WebClient.create(vertx);
        var resp = client.get(port, "127.0.0.1", "/ping").send().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertEquals(200, resp.statusCode());
        assertEquals("pong", resp.bodyAsString());
        service.undeployMe().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }
}
