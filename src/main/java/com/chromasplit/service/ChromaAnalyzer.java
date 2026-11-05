package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

public class ChromaAnalyzer {

    private static final double TIER_BASE_MIN = 0.001;     // 0.1% - mínimo BASE
    private static final double TIER_BASE_MAX = 0.07;      // 7% - máximo BASE
    private static final double TIER_HIGH_THRESHOLD = 0.30;    // 30% - mínimo HIGH

    private static final int DEFAULT_RASTER_DPI = 72;
    private static final int COLOR_TOLERANCE = 10;
    private static final int WHITE_THRESHOLD = 245;
    private static final int BLACK_THRESHOLD = 10;
    private static final int ALPHA_THRESHOLD = 128;

    public List<AnalysisResult.ColorTier> analyzePages(PDDocument document, DoubleConsumer progressCallback) throws IOException {
        int totalPages = document.getNumberOfPages();
        List<AnalysisResult.ColorTier> pageTiers = new ArrayList<>(totalPages);

        for (int i = 0; i < totalPages; i++) {
            PDPage page = document.getPage(i);
            AnalysisResult.ColorTier tier = checkPageForColorTier(document, i, page);
            pageTiers.add(tier);

            if (progressCallback != null) {
                progressCallback.accept((double) (i + 1) / totalPages);
            }
        }

        return pageTiers;
    }

    AnalysisResult.ColorTier checkPageForColorTier(PDDocument document, int pageIndex, PDPage page) throws IOException {
        double colorCoveragePercent = calculateColorCoveragePercentRasterized(document, pageIndex);
        
        if (colorCoveragePercent < TIER_BASE_MIN) {
            // < 0.1% -> B/N (NONE)
            return AnalysisResult.ColorTier.NONE;
        } else if (colorCoveragePercent <= TIER_BASE_MAX) {
            // >=0.1% y <=7% -> BASE
            return AnalysisResult.ColorTier.BASE;
        } else if (colorCoveragePercent < TIER_HIGH_THRESHOLD) {
            // >7% y <30% -> MEDIUM
            return AnalysisResult.ColorTier.MEDIUM;
        } else {
            // >=30% -> HIGH
            return AnalysisResult.ColorTier.HIGH;
        }
    }

    double calculateColorCoveragePercent(PDPage page) throws IOException {
        double pageArea = getPageArea(page);
        if (pageArea <= 0) return 0;

        double colorImageArea = 0;

        PDResources resources = page.getResources();
        if (resources != null) {
            for (var name : resources.getXObjectNames()) {
                var xObj = resources.getXObject(name);
                if (xObj instanceof PDImageXObject imgXObj) {
                    if (imageHasColor(imgXObj)) {
                        colorImageArea += getImageArea(imgXObj);
                    }
                }
            }
        }

        return colorImageArea / pageArea;
    }

    double calculateColorCoveragePercentRasterized(PDDocument document, int pageIndex) throws IOException {
        return calculateColorCoveragePercentRasterized(document, pageIndex, DEFAULT_RASTER_DPI);
    }

    double calculateColorCoveragePercentRasterized(PDDocument document, int pageIndex, int dpi) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);
        return analyzeImageForColorCoverage(image);
    }

    double analyzeImageForColorCoverage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalPixels = (long) width * height;
        if (totalPixels == 0) return 0;

        long colorPixels = 0;
        int[] rgb = new int[3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                rgb[0] = (pixel >> 16) & 0xFF;
                rgb[1] = (pixel >> 8) & 0xFF;
                rgb[2] = pixel & 0xFF;
                int alpha = (pixel >> 24) & 0xFF;

                if (alpha < ALPHA_THRESHOLD) {
                    continue;
                }

                if (isRealColor(rgb)) {
                    colorPixels++;
                }
            }
        }
        return (double) colorPixels / totalPixels;
    }

    boolean isRealColor(int[] rgb) {
        int r = rgb[0], g = rgb[1], b = rgb[2];

        if ((r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD) ||
            (r <= BLACK_THRESHOLD && g <= BLACK_THRESHOLD && b <= BLACK_THRESHOLD)) {
            return false;
        }

        int maxDiff = Math.max(Math.abs(r - g), Math.max(Math.abs(r - b), Math.abs(g - b)));
        return maxDiff > COLOR_TOLERANCE;
    }

    double getPageArea(PDPage page) {
        var mediaBox = page.getMediaBox();
        return mediaBox.getWidth() * mediaBox.getHeight();
    }

    double getImageArea(PDImageXObject image) {
        return image.getWidth() * image.getHeight();
    }

    boolean imageHasColor(PDImageXObject image) throws IOException {
        PDColorSpace colorSpace = image.getColorSpace();
        if (colorSpace == null) return false;

        String csName = colorSpace.getName();
        if ("DeviceRGB".equals(csName) || "DeviceCMYK".equals(csName)) return true;
        if ("ICCBased".equals(csName)) return colorSpace.getNumberOfComponents() >= 3;
        if ("Separation".equals(csName) || "DeviceN".equals(csName)) return true;
        return false;
    }

    boolean checkAnnotationsForColor(PDPage page) throws IOException {
        for (PDAnnotation annotation : page.getAnnotations()) {
            var color = annotation.getColor();
            if (color != null) {
                int rgb = color.toRGB();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                double max = Math.max(Math.max(r, g), b);
                double min = Math.min(Math.min(r, g), b);
                if (max > 0 && (max - min) / max > 0.01) return true;
            }
        }
        return false;
    }

    private static class ColorContentDetector extends PDFGraphicsStreamEngine {
        private boolean foundColor = false;

        ColorContentDetector(PDPage page) {
            super(page);
        }

        boolean hasColor() {
            return foundColor;
        }

    @Override
    public void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        if (foundColor) return;
        String op = operator.getName();
        switch (op) {
            case "RG":
            case "rg":
                if (operands.size() >= 3) {
                    float r = ((COSNumber) operands.get(0)).floatValue();
                    float g = ((COSNumber) operands.get(1)).floatValue();
                    float b = ((COSNumber) operands.get(2)).floatValue();
                    if (Math.abs(r - g) > 0.01f || Math.abs(r - b) > 0.01f) {
                        foundColor = true;
                        return;
                    }
                }
                break;
            case "K":
            case "k":
                if (operands.size() >= 4) {
                    float c = ((COSNumber) operands.get(0)).floatValue();
                    float m = ((COSNumber) operands.get(1)).floatValue();
                    float y = ((COSNumber) operands.get(2)).floatValue();
                    if (c > 0.01f || m > 0.01f || y > 0.01f) {
                        foundColor = true;
                        return;
                    }
                }
                break;
            case "CS":
            case "cs":
                break;
            case "SC":
            case "sc":
            case "SCN":
            case "scn":
                if (operands.size() >= 3) {
                    if (operands.get(0) instanceof COSNumber) {
                        if (operands.size() == 3) {
                            float v1 = ((COSNumber) operands.get(0)).floatValue();
                            float v2 = ((COSNumber) operands.get(1)).floatValue();
                            float v3 = ((COSNumber) operands.get(2)).floatValue();
                            if (Math.abs(v1 - v2) > 0.01f || Math.abs(v1 - v3) > 0.01f) {
                                foundColor = true;
                                return;
                            }
                        } else if (operands.size() == 4) {
                            float c = ((COSNumber) operands.get(0)).floatValue();
                            float m = ((COSNumber) operands.get(1)).floatValue();
                            float y = ((COSNumber) operands.get(2)).floatValue();
                            if (c > 0.01f || m > 0.01f || y > 0.01f) {
                                foundColor = true;
                                return;
                            }
                        } else {
                            foundColor = true;
                            return;
                        }
                    }
                }
                break;
        }
        super.processOperator(operator, operands);
    }

        @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {}
        @Override public void clip(int windingRule) {}
        @Override public void moveTo(float x, float y) {}
        @Override public void lineTo(float x, float y) {}
        @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {}
        @Override public Point2D getCurrentPoint() throws IOException { return new Point2D.Float(0, 0); }
        @Override public void closePath() {}
        @Override public void endPath() {}
        @Override public void drawImage(PDImage pdImage) {}
        @Override public void fillPath(int windingRule) {}
        @Override public void fillAndStrokePath(int windingRule) {}
        @Override public void shadingFill(COSName shadingName) { foundColor = true; }
        @Override public void strokePath() {}
    }

    public List<String> extractPageLabels(PDDocument document) throws IOException {
        var catalog = document.getDocumentCatalog().getCOSObject();
        if (!catalog.containsKey(COSName.PAGE_LABELS)) {
            return null;
        }
        var pageLabels = document.getDocumentCatalog().getPageLabels();
        if (pageLabels == null) {
            return null;
        }
        String[] labelsByPageIndex = pageLabels.getLabelsByPageIndices();
        if (labelsByPageIndex == null || labelsByPageIndex.length == 0) {
            return null;
        }
        List<String> labels = new ArrayList<>(document.getNumberOfPages());
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            String label = i < labelsByPageIndex.length ? labelsByPageIndex[i] : null;
            labels.add(label);
        }
        return labels;
    }
}
