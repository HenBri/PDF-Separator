package com.chromasplit.model;

import java.util.List;

public class AnalysisResult {
    private final int totalPages;
    private final int bwPages;
    private final int colorPages;
    private final int colorBasePages;
    private final int colorMediumPages;
    private final int colorHighPages;
    private final List<Integer> colorPageIndices;
    private final List<String> colorPageLabels;
    private final List<ColorTier> colorPageTiers;

    public enum ColorTier {
        NONE("B/N", 0), BASE("Color bajo", 0.001), MEDIUM("Color medio", 0.35), HIGH("Color alto", 0.65);

        private final String label;
        private final double boundary;

        ColorTier(String label, double boundary) {
            this.label = label;
            this.boundary = boundary;
        }

        public String getLabel() { return label; }

        private static String percent(double value) {
            return java.math.BigDecimal.valueOf(value).multiply(java.math.BigDecimal.valueOf(100))
                    .stripTrailingZeros().toPlainString().replace('.', ',') + " %";
        }

        public String getCoverageRange() {
            return switch (this) {
                case NONE -> "Menor al " + percent(BASE.boundary);
                case BASE -> percent(BASE.boundary) + " ≤ cobertura ≤ " + percent(MEDIUM.boundary);
                case MEDIUM -> percent(MEDIUM.boundary) + " < cobertura < " + percent(HIGH.boundary);
                case HIGH -> "Cobertura ≥ " + percent(HIGH.boundary);
            };
        }

        public static ColorTier fromCoverage(double coverage) {
            if (!Double.isFinite(coverage) || coverage < 0 || coverage > 1) {
                throw new IllegalArgumentException("Cobertura fuera de rango");
            }
            if (coverage < BASE.boundary) return NONE;
            if (coverage <= MEDIUM.boundary) return BASE;
            if (coverage < HIGH.boundary) return MEDIUM;
            return HIGH;
        }
    }

    public AnalysisResult(int totalPages, int bwPages, int colorPages,
                          int colorBasePages, int colorMediumPages, int colorHighPages,
                          List<Integer> colorPageIndices, List<String> colorPageLabels,
                          List<ColorTier> colorPageTiers) {
        this.totalPages = totalPages;
        this.bwPages = bwPages;
        this.colorPages = colorPages;
        this.colorBasePages = colorBasePages;
        this.colorMediumPages = colorMediumPages;
        this.colorHighPages = colorHighPages;
        this.colorPageIndices = colorPageIndices;
        this.colorPageLabels = colorPageLabels;
        this.colorPageTiers = colorPageTiers;
    }

    public int getTotalPages() { return totalPages; }
    public int getBwPages() { return bwPages; }
    public int getColorPages() { return colorPages; }
    public int getColorBasePages() { return colorBasePages; }
    public int getColorMediumPages() { return colorMediumPages; }
    public int getColorHighPages() { return colorHighPages; }
    public List<Integer> getColorPageIndices() { return colorPageIndices; }
    public List<String> getColorPageLabels() { return colorPageLabels; }
    public List<ColorTier> getColorPageTiers() { return colorPageTiers; }

    public boolean hasPageLabels() {
        if (colorPageLabels == null || colorPageLabels.isEmpty()) return false;
        for (String label : colorPageLabels) {
            if (label != null) return true;
        }
        return false;
    }

    public int getColorPageCountByTier(ColorTier tier) {
        return switch (tier) {
            case BASE -> colorBasePages;
            case MEDIUM -> colorMediumPages;
            case HIGH -> colorHighPages;
            default -> 0;
        };
    }
}
