package com.quitmate.global.appserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * 테스트용 가짜 앱 서버. 다음 응답(상태 코드·본문)을 정해 두고, 받은 요청을 기록한다.
 */
public class AppServerStub implements AutoCloseable {

    private final HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = "{}";
    private volatile long delayMillis = 0;

    public AppServerStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void respond(int status, String body) {
        this.status = status;
        this.body = body;
        this.delayMillis = 0;
    }

    public void respondSlowly(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public List<Received> received() {
        return received;
    }

    public Received lastReceived() {
        return received.get(received.size() - 1);
    }

    public void reset() {
        received.clear();
        respond(200, "{}");
    }

    private void handle(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization"), requestBody));
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public record Received(String method, String uri, String authorization, String body) {
    }
}
