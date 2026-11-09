package com.chromasplit.service;

import com.chromasplit.model.AnalysisResult.ColorTier;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class TieredPricingTest {
    @Test
    void classifiesExactBoundariesWithoutGaps() {
        assertEquals(ColorTier.NONE, ColorTier.fromCoverage(0));
        assertEquals(ColorTier.NONE, ColorTier.fromCoverage(Math.nextDown(0.001)));
        assertEquals(ColorTier.BASE, ColorTier.fromCoverage(0.001));
        assertEquals(ColorTier.BASE, ColorTier.fromCoverage(Math.nextDown(0.35)));
        assertEquals(ColorTier.BASE, ColorTier.fromCoverage(0.35));
        assertEquals(ColorTier.MEDIUM, ColorTier.fromCoverage(Math.nextUp(0.35)));
        assertEquals(ColorTier.MEDIUM, ColorTier.fromCoverage(Math.nextDown(0.65)));
        assertEquals(ColorTier.HIGH, ColorTier.fromCoverage(0.65));
        assertEquals(ColorTier.HIGH, ColorTier.fromCoverage(Math.nextUp(0.65)));
        assertEquals(ColorTier.HIGH, ColorTier.fromCoverage(1));
    }

    @Test
    void analyzesKnownCoverageAndChargesEachTierWithPersistedRates() throws Exception {
        Preferences preferences = Preferences.userRoot().node("chromasplit-test-" + UUID.randomUUID());
        try (PDDocument document = new PDDocument()) {
            for (int coverage : new int[]{0, 35, 50, 65, 100}) {
                PDPage page = new PDPage(new PDRectangle(100, 100));
                document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    content.setNonStrokingColor(java.awt.Color.RED);
                    content.addRect(0, 0, coverage, 100);
                    content.fill();
                }
            }
            var result = new PdfSplitter(new ChromaAnalyzer()).analyze(document, null);
            assertEquals(1, result.getBwPages());
            assertEquals(1, result.getColorBasePages());
            assertEquals(1, result.getColorMediumPages());
            assertEquals(2, result.getColorHighPages());
            assertEquals(result.getColorPages(), result.getColorBasePages() + result.getColorMediumPages() + result.getColorHighPages());
            var prices = new PreferencesPriceStorage(preferences);
            prices.saveBwPrice(0.10);
            prices.saveColorBasePrice(0.20);
            prices.saveColorMediumPrice(0.30);
            prices.saveColorHighPrice(0.40);
            prices = new PreferencesPriceStorage(preferences);
            var calculator = new PricingCalculator();
            var breakdown = calculator.calculate(result, prices, Money.parse("0,10"));
            assertEquals(new BigDecimal("1.30"), breakdown.colorTotal());
            assertEquals(new BigDecimal("1.50"), breakdown.total());
            prices.saveColorHighPrice(0.50);
            assertEquals(new BigDecimal("1.70"), calculator.calculate(result, prices, Money.parse("0.10")).total());
            assertEquals(new BigDecimal("0.00"), calculator.calculate(null, prices, Money.parse("0")).total());
        } finally {
            preferences.removeNode();
        }
    }

    @Test
    void validatesMoneyAndRoundsStoredRates() {
        assertEquals(new BigDecimal("12.30"), Money.parse(" 12,3 "));
        assertEquals(new BigDecimal("1.01"), Money.fromStored(1.005));
        for (String invalid : List.of("NaN", "Infinity", "-1", "1.234", "", "1e3")) {
            assertThrows(IllegalArgumentException.class, () -> Money.parse(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> Money.fromStored(Double.POSITIVE_INFINITY));
    }
}
