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
        NONE, BASE, MEDIUM, HIGH
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