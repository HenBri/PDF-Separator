package com.chromasplit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfBrowserServiceTest {
    @TempDir Path directory;

    @Test
    void servesEachDocumentInlineAndInvalidatesPreviousLinks() throws Exception {
        Path color = Files.writeString(directory.resolve("documento á color.pdf"), "%PDF-1.7\ncolor");
        Path bw = Files.writeString(directory.resolve("documento B N.pdf"), "%PDF-1.7\nblanco y negro");
        try (var service = new PdfBrowserService()) {
            URI colorUri = service.publish(color);
            URI bwUri = service.publish(bw);
            assertEquals("127.0.0.1", colorUri.getHost());
            assertPdf(colorUri, Files.readAllBytes(color));
            assertPdf(bwUri, Files.readAllBytes(bw));
            service.clear();
            assertEquals(404, status(colorUri));
            assertEquals(404, status(bwUri));
        }
    }

    @Test
    void handlesMissingFilesAndUnknownRoutes() throws Exception {
        try (var service = new PdfBrowserService()) {
            assertThrows(IOException.class, () -> service.publish(directory.resolve("missing.pdf")));
            Path file = Files.writeString(directory.resolve("sample.pdf"), "%PDF-1.7");
            URI uri = service.publish(file);
            assertEquals(404, status(uri.resolve("/unknown.pdf")));
            Files.delete(file);
            assertEquals(404, status(uri));
        }
    }

    private void assertPdf(URI uri, byte[] expected) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            assertEquals(200, connection.getResponseCode());
            assertEquals("application/pdf", connection.getContentType());
            assertEquals("inline", connection.getHeaderField("Content-Disposition"));
            try (var input = connection.getInputStream()) {
                assertArrayEquals(expected, input.readAllBytes());
            }
        } finally {
            connection.disconnect();
        }
    }

    private int status(URI uri) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
