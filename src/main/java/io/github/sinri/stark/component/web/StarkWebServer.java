package io.github.sinri.stark.component.web;

import io.github.sinri.stark.component.verticles.StarkVerticleBase;
import io.github.sinri.stark.core.LateObject;
import io.github.sinri.stark.core.Stark;
import io.github.sinri.stark.logging.base.Logger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public abstract class StarkWebServer extends StarkVerticleBase {
    private final Logger httpServerLogger;
    private final LateObject<HttpServer> lateHttpServer = new LateObject<>();

    public StarkWebServer(HttpServerOptions httpServerOptions, Logger httpServerLogger) {
        this.httpServerLogger = httpServerLogger;
    }

    protected abstract HttpServerOptions buildHttpServerOptions();

    protected abstract void configureRoutes(Router router);

    /**
     * Executes tasks or setup logic that needs to be completed before the HTTP server starts.
     * This method returns a future that signifies the completion of any preparatory operations
     * required prior to initializing the server.
     *
     * @return a {@link Future} that completes with {@code null} upon successful completion of all pre-server-start
     *         tasks,
     *         or fails with an exception if any errors occur during the process.
     */
    protected Future<Void> beforeStartServer() {
        return Future.succeededFuture();
    }

    /**
     * Executes tasks or cleanup operations after the HTTP server has shut down.
     * This method is invoked to perform any necessary post-shutdown activities,
     * such as releasing resources or logging the shutdown event.
     *
     * @return a {@link Future} that completes with {@code null} upon the successful
     *         completion of all post-shutdown tasks, or fails with an exception if
     *         an error occurs during the execution of these tasks.
     */
    protected Future<Void> afterShutdownServer() {
        return Future.succeededFuture();
    }


    public Logger getHttpServerLogger() {
        return httpServerLogger;
    }

    @Override
    protected Future<?> startVerticle() {
        HttpServer server = getStark().createHttpServer(buildHttpServerOptions());
        this.lateHttpServer.initialize(server);

        Router router = Router.router(getStark());
        this.configureRoutes(router);

        return beforeStartServer()
                .compose(v0 -> server
                        .requestHandler(router)
                        .exceptionHandler(throwable -> getHttpServerLogger().error(r -> r
                                .setMessage("KeelHttpServer Exception")
                                .setThrowable(throwable)))
                        .listen()
                        .compose(httpServer -> {
                            int actualPort = httpServer.actualPort();
                            getHttpServerLogger().info(r -> r
                                    .setMessage("HTTP Server Established, Actual Port: " + actualPort));
                            return Future.succeededFuture();
                        }, throwable -> {
                            getHttpServerLogger().error(r -> r
                                    .setMessage("Listen failed")
                                    .setThrowable(throwable));
                            return Future.failedFuture(throwable);
                        }));
    }

    @Override
    protected Future<Void> stopVerticle() {
        if (!lateHttpServer.isInitialized()) {
            return Future.succeededFuture();
        }
        var server = lateHttpServer.get();
        return server.close()
                     .compose(v -> {
                         getHttpServerLogger().info(r -> r.setMessage("HTTP Server Closed"));
                         return afterShutdownServer()
                                 .recover(throwable2 -> {
                                     getHttpServerLogger().error(r -> r
                                             .setMessage("afterShutdownServer failed: %s".formatted(throwable2.getMessage()))
                                             .setThrowable(throwable2));
                                     return Future.succeededFuture();
                                 });
                     }, throwable -> {
                         getHttpServerLogger().error(r -> r
                                 .setMessage("HTTP Server Closing Failure: %s".formatted(throwable.getMessage()))
                                 .setThrowable(throwable));
                         return Future.failedFuture(throwable);
                     });
    }

    /**
     * Deploys the current verticle with an appropriate threading model configuration,
     * which by default is VIRTUAL_THREAD.
     *
     * @return a {@link Future} that completes with the deployment ID if the deployment is successful,
     *         or fails with an exception if the deployment fails.
     */
    public Future<String> deployMe(Stark stark) {
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
        return super.deployMe(stark, deploymentOptions);
    }
}
