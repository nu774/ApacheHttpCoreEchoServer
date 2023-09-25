package acme;

import acme.http.ServerBootstrap;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ExceptionListener;
import org.apache.hc.core5.http.HttpConnection;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.impl.Http1StreamListener;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.io.IOCallback;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;

public class EchoHandler implements HttpRequestHandler, Http1StreamListener, ExceptionListener {
    private static final Logger logger = LoggerFactory.getLogger(EchoHandler.class);
    @Override
    public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context) {
        if (!request.getMethod().startsWith("P"))
            response.setEntity(HttpEntities.create(""));
        else {
            final var requestEntity = request.getEntity();
            final IOCallback<OutputStream> callback = (sink) -> requestEntity.getContent().transferTo(sink);
            final var contentType = Optional.ofNullable(requestEntity.getContentType())
                    .map(ContentType::create)
                    .orElse(ContentType.APPLICATION_OCTET_STREAM);
            response.setEntity(HttpEntities.create(callback, contentType));
        }
        response.setCode(200);
    }

    @Override
    public void onRequestHead(HttpConnection connection, HttpRequest request) {
        logger.info("{}: {} {}", connection.getRemoteAddress(), request.getMethod(), request.getPath());
    }
    @Override
    public void onResponseHead(HttpConnection connection, HttpResponse response) {
        logger.info("{}: {} {}", connection.getRemoteAddress(), response.getCode(), response.getReasonPhrase());
    }
    @Override
    public void onExchangeComplete(HttpConnection connection, boolean keepAlive) {
        logger.info("{}: exchange completed: keepAlive={}", connection.getRemoteAddress(), keepAlive);
    }
    @Override
    public void onError(Exception ex) {
        logger.error("", ex);
    }

    @Override
    public void onError(HttpConnection connection, Exception ex) {
        logger.error("{}", connection.getRemoteAddress(), ex);
    }

    public static void main(String[] args) throws Exception {
        var listenAddress = new InetSocketAddress(8080);
        if (args.length > 0) {
            URI uri = new URI("http://" + args[0]);
            listenAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
        }
        final var socketConfig = SocketConfig
                .custom()
                .setSoReuseAddress(true)
                .setBacklogSize(1024)
                .build();
        final var http1Config = Http1Config
                .custom()
                .setChunkSizeHint(1) // We need this for immediate echo back
                .build();
        final var handler = new EchoHandler();
        final var server = ServerBootstrap.bootstrap()
                .setLocalAddress(listenAddress.getAddress())
                .setListenerPort(listenAddress.getPort())
                .setSocketConfig(socketConfig)
                .setHttp1Config(http1Config)
                .setStreamListener(handler)
                .setExceptionListener(handler)
                .register("*", handler)
                .create();
        server.start();
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> server.close(CloseMode.IMMEDIATE)));
        handler.logger.info("listening at {}:{}", server.getInetAddress().getHostAddress(), server.getLocalPort());
        server.awaitTermination(TimeValue.MAX_VALUE);
    }
}
