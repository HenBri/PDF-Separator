package com.chromasplit.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PdfBrowserService implements AutoCloseable {
    private final Map<String, Path> documents = new ConcurrentHashMap<>();
    private HttpServer server;

    public void open(Path file) throws IOException {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("No se puede abrir el navegador predeterminado en este equipo.");
        }
        Desktop.getDesktop().browse(publish(file));
    }

    URI publish(Path file) throws IOException {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IOException("Archivo no encontrado o no legible: " + file.getFileName());
        }
        if (server == null) {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::serve);
            server.start();
        }
        String route = "/" + UUID.randomUUID() + "/document.pdf";
        documents.put(route, file.toAbsolutePath());
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + route);
    }

    private void serve(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Path file = documents.get(exchange.getRequestURI().getPath());
            if (file == null || !Files.isRegularFile(file) || !Files.isReadable(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            try (var input = Files.newInputStream(file)) {
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.getResponseHeaders().set("Content-Disposition", "inline");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.getResponseHeaders().set("Content-Length", Long.toString(Files.size(file)));
                exchange.sendResponseHeaders(200, method.equals("HEAD") ? -1 : Files.size(file));
                if (method.equals("GET")) input.transferTo(exchange.getResponseBody());
            }
        }
    }

    public void clear() {
        documents.clear();
    }

    @Override
    public void close() {
        clear();
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
