package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.DoubleConsumer;

public class PdfService {

    private final PdfSplitter splitter;
    private PDDocument currentDocument;
    private AnalysisResult currentResult;

    public PdfService(PdfSplitter splitter) {
        this.splitter = splitter;
    }

    public AnalysisResult loadAndAnalyze(File file, DoubleConsumer progressCallback) throws IOException {
        close();
        currentDocument = Loader.loadPDF(file);
        currentResult = splitter.analyze(currentDocument, progressCallback);
        return currentResult;
    }

    public void export(Path outputDir, String baseName, DoubleConsumer progressCallback) throws IOException {
        if (currentDocument == null || currentDocument.getDocument().isClosed()) {
            throw new IllegalStateException("El documento PDF ya fue cerrado. Cargue el archivo nuevamente.");
        }
        splitter.split(currentDocument, outputDir, baseName, progressCallback);
    }

    public void close() {
        if (currentDocument != null) {
            try {
                currentDocument.close();
            } catch (IOException ignored) {
            }
            currentDocument = null;
        }
        currentResult = null;
        splitter.reset();
    }

    public boolean hasDocument() {
        return currentDocument != null;
    }

    public AnalysisResult getResult() {
        return currentResult;
    }
}
