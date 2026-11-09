package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult;
import java.math.BigDecimal;

public class PricingCalculator {

    public record Breakdown(BigDecimal bw, BigDecimal low, BigDecimal medium, BigDecimal high,
                            BigDecimal extras) {
        public BigDecimal colorTotal() { return low.add(medium).add(high); }
        public BigDecimal total() { return bw.add(colorTotal()).add(extras); }
    }

    public Breakdown calculate(AnalysisResult result, PriceStorage prices, BigDecimal extras) {
        return new Breakdown(
                Money.fromStored(prices.loadBwPrice()).multiply(BigDecimal.valueOf(result == null ? 0 : result.getBwPages())),
                Money.fromStored(prices.loadColorBasePrice()).multiply(BigDecimal.valueOf(result == null ? 0 : result.getColorBasePages())),
                Money.fromStored(prices.loadColorMediumPrice()).multiply(BigDecimal.valueOf(result == null ? 0 : result.getColorMediumPages())),
                Money.fromStored(prices.loadColorHighPrice()).multiply(BigDecimal.valueOf(result == null ? 0 : result.getColorHighPages())),
                extras);
    }

    public double calculateBwTotal(int bwPages, double bwPrice) {
        return bwPrice * bwPages;
    }

    public double calculateColorTotal(int colorPages, double colorPrice) {
        return colorPrice * colorPages;
    }

    public double calculateColorTierTotal(int basePages, double basePrice,
                                           int mediumPages, double mediumPrice,
                                           int highPages, double highPrice) {
        return basePages * basePrice + mediumPages * mediumPrice + highPages * highPrice;
    }

    public double calculateTotal(int bwPages, int colorPages, double bwPrice, double colorPrice, double extras) {
        return calculateBwTotal(bwPages, bwPrice) + calculateColorTotal(colorPages, colorPrice) + extras;
    }

    public double calculateTotalTiered(int bwPages, int colorBasePages, int colorMediumPages, int colorHighPages,
                                        double bwPrice, double colorBasePrice, double colorMediumPrice, double colorHighPrice,
                                        double extras) {
        return calculateBwTotal(bwPages, bwPrice)
                + calculateColorTierTotal(colorBasePages, colorBasePrice, colorMediumPages, colorMediumPrice, colorHighPages, colorHighPrice)
                + extras;
    }
}
