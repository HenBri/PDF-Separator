package com.chromasplit.service;

import java.util.prefs.Preferences;

public class PreferencesPriceStorage implements PriceStorage {
    private static final String KEY_BW = "bw_price";
    private static final String KEY_COLOR = "color_price";
    private static final String KEY_COLOR_BASE = "color_base_price";
    private static final String KEY_COLOR_MEDIUM = "color_medium_price";
    private static final String KEY_COLOR_HIGH = "color_high_price";
    private static final double DEFAULT_BW = 0.50;
    private static final double DEFAULT_COLOR = 1.50;
    private static final double DEFAULT_COLOR_BASE = 0.80;
    private static final double DEFAULT_COLOR_MEDIUM = 1.50;
    private static final double DEFAULT_COLOR_HIGH = 3.00;

    private final Preferences prefs = Preferences.userNodeForPackage(PreferencesPriceStorage.class);

    @Override
    public double loadBwPrice() {
        return prefs.getDouble(KEY_BW, DEFAULT_BW);
    }

    @Override
    public double loadColorPrice() {
        return prefs.getDouble(KEY_COLOR, DEFAULT_COLOR);
    }

    @Override
    public double loadColorBasePrice() {
        return prefs.getDouble(KEY_COLOR_BASE, DEFAULT_COLOR_BASE);
    }

    @Override
    public double loadColorMediumPrice() {
        return prefs.getDouble(KEY_COLOR_MEDIUM, DEFAULT_COLOR_MEDIUM);
    }

    @Override
    public double loadColorHighPrice() {
        return prefs.getDouble(KEY_COLOR_HIGH, DEFAULT_COLOR_HIGH);
    }

    @Override
    public void saveBwPrice(double price) {
        prefs.putDouble(KEY_BW, Math.max(0, price));
    }

    @Override
    public void saveColorPrice(double price) {
        prefs.putDouble(KEY_COLOR, Math.max(0, price));
    }

    @Override
    public void saveColorBasePrice(double price) {
        prefs.putDouble(KEY_COLOR_BASE, Math.max(0, price));
    }

    @Override
    public void saveColorMediumPrice(double price) {
        prefs.putDouble(KEY_COLOR_MEDIUM, Math.max(0, price));
    }

    @Override
    public void saveColorHighPrice(double price) {
        prefs.putDouble(KEY_COLOR_HIGH, Math.max(0, price));
    }
}
