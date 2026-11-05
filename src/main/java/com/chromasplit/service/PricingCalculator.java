package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult;

public class PricingCalculator {

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
