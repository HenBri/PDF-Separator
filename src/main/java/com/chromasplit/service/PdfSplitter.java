package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public class PdfSplitter {

    private final ChromaAnalyzer analyzer;
    private List<AnalysisResult.ColorTier> pageColorTiers;

    public PdfSplitter(ChromaAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public List<AnalysisResult.ColorTier> getPageColorTiers() {
        return pageColorTiers;
    }

    public AnalysisResult analyze(PDDocument document, DoubleConsumer progressCallback) throws IOException {
        List<AnalysisResult.ColorTier> tiers = analyzer.analyzePages(document, progressCallback);
        this.pageColorTiers = tiers;

        List<Integer> colorIndices = new ArrayList<>();
        List<AnalysisResult.ColorTier> colorTiers = new ArrayList<>();
        int colorBasePages = 0;
        int colorMediumPages = 0;
        int colorHighPages = 0;

        for (int i = 0; i < tiers.size(); i++) {
            AnalysisResult.ColorTier tier = tiers.get(i);
            if (tier != AnalysisResult.ColorTier.NONE) {
                colorIndices.add(i + 1);
                colorTiers.add(tier);
                switch (tier) {
                    case BASE -> colorBasePages++;
                    case MEDIUM -> colorMediumPages++;
                    case HIGH -> colorHighPages++;
                }
            }
        }

        List<String> pageLabels = analyzer.extractPageLabels(document);
        boolean hasPageLabels = pageLabels != null;
        List<String> colorLabels = new ArrayList<>();
        for (int i = 0; i < tiers.size(); i++) {
            if (tiers.get(i) != AnalysisResult.ColorTier.NONE) {
                colorLabels.add(hasPageLabels ? pageLabels.get(i) : null);
            }
        }

        int total = tiers.size();
        int color = colorIndices.size();
        int bw = total - color;

        return new AnalysisResult(total, bw, color, colorBasePages, colorMediumPages, colorHighPages,
                colorIndices, colorLabels, colorTiers);
    }

    public void reset() {
        pageColorTiers = null;
    }

    public void split(PDDocument document, Path outputDir, String baseName,
                      DoubleConsumer progressCallback) throws IOException {
        if (pageColorTiers == null) {
            throw new IllegalStateException("Debe ejecutar el análisis antes de exportar (analyze() antes de split())");
        }
        if (document == null || document.getDocument().isClosed()) {
            throw new IllegalStateException("El documento PDF ya fue cerrado. Cargue el archivo nuevamente.");
        }

        PDDocument bwDoc = new PDDocument();
        PDDocument colorDoc = new PDDocument();
        int total = pageColorTiers.size();

        try {
            for (int i = 0; i < total; i++) {
                var page = document.getPage(i);
                if (pageColorTiers.get(i) != AnalysisResult.ColorTier.NONE) {
                    colorDoc.addPage(page);
                } else {
                    bwDoc.addPage(page);
                }
                if (progressCallback != null) {
                    progressCallback.accept((double) (i + 1) / total);
                }
            }

            if (bwDoc.getNumberOfPages() > 0) {
                bwDoc.save(outputDir.resolve(baseName + "_BN.pdf").toFile());
            }
            if (colorDoc.getNumberOfPages() > 0) {
                colorDoc.save(outputDir.resolve(baseName + "_COLOR.pdf").toFile());
            }
        } finally {
            bwDoc.close();
            colorDoc.close();
        }
    }
}