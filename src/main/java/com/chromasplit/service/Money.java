package com.chromasplit.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {}

    public static BigDecimal parse(String text) {
        String normalized = text == null ? "" : text.trim().replace(',', '.');
        if (!normalized.matches("\\d+(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException("Ingresa un importe no negativo con hasta dos decimales.");
        }
        BigDecimal value = new BigDecimal(normalized);
        if (!Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Importe demasiado grande.");
        return value.setScale(2);
    }

    public static BigDecimal fromStored(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Precio inválido");
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
