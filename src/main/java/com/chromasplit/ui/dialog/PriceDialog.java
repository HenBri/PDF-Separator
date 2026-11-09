package com.chromasplit.ui.dialog;

import com.chromasplit.service.PriceStorage;
import com.chromasplit.service.Money;
import com.chromasplit.model.AnalysisResult.ColorTier;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Locale;

public class PriceDialog {

    private final PriceStorage storage;

    public PriceDialog(PriceStorage storage) {
        this.storage = storage;
    }

    public void showAndWait() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configurar Precios");
        dialog.setHeaderText("Precio por página según su cobertura de color");

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField bwField = new TextField(String.format(Locale.US, "%.2f", storage.loadBwPrice()));
        TextField colorBaseField = new TextField(String.format(Locale.US, "%.2f", storage.loadColorBasePrice()));
        TextField colorMediumField = new TextField(String.format(Locale.US, "%.2f", storage.loadColorMediumPrice()));
        TextField colorHighField = new TextField(String.format(Locale.US, "%.2f", storage.loadColorHighPrice()));

        grid.add(new Label(ColorTier.NONE.getLabel()), 0, 0);
        grid.add(bwField, 1, 0);
        grid.add(new Label(ColorTier.BASE.getLabel()), 0, 1);
        grid.add(colorBaseField, 1, 1);
        grid.add(new Label(ColorTier.MEDIUM.getLabel()), 0, 2);
        grid.add(colorMediumField, 1, 2);
        grid.add(new Label(ColorTier.HIGH.getLabel()), 0, 3);
        grid.add(colorHighField, 1, 3);
        for (ColorTier tier : ColorTier.values()) {
            grid.add(new Label(tier.getCoverageRange()), 2, tier.ordinal());
        }
        Label explanation = new Label("Cobertura: superficie de la página identificada como color.\n"
                + "Estimación visual; no mide el consumo exacto de tinta. Precios por página.");
        explanation.setWrapText(true);
        grid.add(explanation, 0, 4, 3, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return saveButtonType;
            }
            return null;
        });

        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (!isValidPrice(bwField.getText()) || !isValidPrice(colorBaseField.getText())
                            || !isValidPrice(colorMediumField.getText()) || !isValidPrice(colorHighField.getText())) {
                        showAlert("Precio inválido", "Ingresa importes no negativos con hasta dos decimales (coma o punto).");
                        event.consume();
                    }
                });

        dialog.showAndWait().ifPresent(button -> {
            if (button == saveButtonType) {
                storage.saveBwPrice(Double.parseDouble(bwField.getText().replace(',', '.')));
                storage.saveColorBasePrice(Double.parseDouble(colorBaseField.getText().replace(',', '.')));
                storage.saveColorMediumPrice(Double.parseDouble(colorMediumField.getText().replace(',', '.')));
                storage.saveColorHighPrice(Double.parseDouble(colorHighField.getText().replace(',', '.')));
            }
        });
    }

    private boolean isValidPrice(String value) {
        if (value == null) return false;
        try {
            Money.parse(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
