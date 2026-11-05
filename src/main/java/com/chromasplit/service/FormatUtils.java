package com.chromasplit.service;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class FormatUtils {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private FormatUtils() {}

    public static String formatPrice(double value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double parseDoubleSafe(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static String formatList(List<?> items) {
        if (items == null || items.isEmpty()) return "Ninguna";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    public static String stripExtension(String name) {
        if (name == null) return "output";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
