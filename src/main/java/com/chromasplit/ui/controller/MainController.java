package com.chromasplit.ui.controller;

import com.chromasplit.model.AnalysisResult;
import com.chromasplit.model.PdfState;
import com.chromasplit.service.*;
import com.chromasplit.ui.dialog.PriceDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.chromasplit.service.FormatUtils.*;

public class MainController {

    @FXML private VBox dropZone;
    @FXML private Label fileNameLabel;
    @FXML private Label totalLabel;
    @FXML private Label bwLabel;
    @FXML private Label colorLabel;
    @FXML private Label physicalIdxLabel;
    @FXML private Label totalBwPriceLabel;
    @FXML private Label totalColorPriceLabel;
    @FXML private Label totalPriceLabel;
    @FXML private TextField extrasField;
    @FXML private ProgressBar progressBar;
    @FXML private ProgressBar exportProgressBar;
    @FXML private Label statusLabel;
    @FXML private Button exportButton;
    @FXML private Button clearButton;

    @FXML private VBox resultsPanel;
    @FXML private VBox colorCard;
    @FXML private VBox bwCard;
    @FXML private Label colorFileNameLabel;
    @FXML private Label bwFileNameLabel;
    @FXML private Button openColorButton;
    @FXML private Button openBwButton;

    private final PdfState state = new PdfState();
    private final PricingCalculator pricingCalculator = new PricingCalculator();
    private PdfService pdfService;
    private PriceStorage priceStorage;
    private ExportStorage exportStorage;
    private File lastExportDir;

    public MainController(PriceStorage priceStorage, ExportStorage exportStorage, PdfService pdfService) {
        this.priceStorage = priceStorage;
        this.exportStorage = exportStorage;
        this.pdfService = pdfService;
    }

    @FXML
    public void initialize() {
        exportButton.setDisable(true);
        progressBar.setProgress(0);
        exportProgressBar.setProgress(0);
        exportProgressBar.setVisible(false);
        String savedDir = exportStorage.loadLastExportDir();
        if (!savedDir.isEmpty()) {
            File f = new File(savedDir);
            if (f.isDirectory()) lastExportDir = f;
        }
        setupDragDrop();
        bindUI();
        extrasField.textProperty().addListener((obs, old, val) -> updatePriceDisplay());
    }

    private void bindUI() {
        state.statusProperty().addListener((obs, old, val) -> statusLabel.setText(val));
        state.fileNameProperty().addListener((obs, old, val) -> fileNameLabel.setText(val));
        state.runningProperty().addListener((obs, old, val) -> {
            boolean hasDoc = pdfService.hasDocument();
            exportButton.setDisable(val || !hasDoc);
            clearButton.setDisable(val);
            dropZone.setDisable(val);
        });
    }

    private void setupDragDrop() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleDragDropped);
        dropZone.setOnMouseClicked(e -> openFileChooser());
    }

    private void handleDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() && db.getFiles().size() == 1
                && db.getFiles().get(0).getName().toLowerCase().endsWith(".pdf")) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            loadAndAnalyze(db.getFiles().get(0));
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"));
        fileChooser.setTitle("Seleccionar archivo PDF");
        File file = fileChooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            loadAndAnalyze(file);
        }
    }

    private void loadAndAnalyze(File file) {
        progressBar.progressProperty().unbind();
        exportProgressBar.progressProperty().unbind();
        pdfService.close();
        state.reset();
        resetUI();
        state.setFileName(file.getName());
        state.setRunning(true);
        state.setStatus("Analizando documento...");

        Task<Void> analysisTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                AnalysisResult result = pdfService.loadAndAnalyze(file, p -> updateProgress(p, 1.0));
                Platform.runLater(() -> displayResults(result));
                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.progressProperty().unbind();
                    pdfService.close();
                    state.reset();
                    resetUI();
                    exportButton.setDisable(true);
                    state.setStatus("Error: " + getException().getMessage());
                });
            }
        };

        progressBar.progressProperty().bind(analysisTask.progressProperty());
        new Thread(analysisTask).start();
    }

    private void displayResults(AnalysisResult result) {
        totalLabel.setText(String.valueOf(result.getTotalPages()));
        bwLabel.setText(String.valueOf(result.getBwPages()));
        colorLabel.setText(String.valueOf(result.getColorPages()));
        physicalIdxLabel.setText(formatList(result.getColorPageIndices()));
        updatePriceDisplay(result.getBwPages(), result.getColorPages());
        state.setRunning(false);
        state.setComplete(true);
        state.setStatus("Análisis completado. Listo para exportar.");
        exportButton.setDisable(false);
        
        String baseName = stripExtension(state.getFileName());
        showResultsPanel(baseName);
    }

    @FXML
    private void handleExport() {
        if (lastExportDir == null || !lastExportDir.isDirectory()) {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("Seleccionar carpeta de destino");
            if (lastExportDir != null) dirChooser.setInitialDirectory(lastExportDir);
            File dir = dirChooser.showDialog(dropZone.getScene().getWindow());
            if (dir == null) return;
            lastExportDir = dir;
            exportStorage.saveLastExportDir(dir.getAbsolutePath());
        }

        state.setRunning(true);
        state.setStatus("Exportando documentos...");
        state.setOutputDir(lastExportDir.getAbsolutePath());

        String baseName = stripExtension(state.getFileName());

        exportProgressBar.setVisible(true);
        exportProgressBar.setProgress(0);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                pdfService.export(lastExportDir.toPath(), baseName, p -> updateProgress(p, 1.0));
                return null;
            }

@Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        exportProgressBar.progressProperty().unbind();
                        state.setRunning(false);
                        state.setStatus("Documentos exportados correctamente.");
                        exportProgressBar.setProgress(1.0);
                        checkAndEnableOpenButtons();
                    });
                }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    exportProgressBar.progressProperty().unbind();
                    state.setRunning(false);
                    state.setStatus("Error al exportar: " + getException().getMessage());
                    exportProgressBar.setVisible(false);
                });
            }
        };

        exportProgressBar.progressProperty().unbind();
        exportProgressBar.progressProperty().bind(exportTask.progressProperty());
        new Thread(exportTask).start();
    }

    @FXML
    private void handleChangeExportDir() {
        lastExportDir = null;
        openColorButton.setDisable(true);
        openBwButton.setDisable(true);
        handleExport();
    }

    @FXML
    private void handleOpenPrices() {
        new PriceDialog(priceStorage).showAndWait();
        updatePriceDisplay();
    }

    @FXML
    private void handleClear() {
        progressBar.progressProperty().unbind();
        exportProgressBar.progressProperty().unbind();
        pdfService.close();
        state.reset();
        resetUI();
        exportButton.setDisable(true);
    }

    private void resetUI() {
        totalLabel.setText("0");
        bwLabel.setText("0");
        colorLabel.setText("0");
        physicalIdxLabel.setText("—");
        totalBwPriceLabel.setText(formatPrice(0));
        totalColorPriceLabel.setText(formatPrice(0));
        totalPriceLabel.setText(formatPrice(0));
        extrasField.setText("0");
        progressBar.setProgress(0);
        exportProgressBar.setProgress(0);
        exportProgressBar.setVisible(false);
        hideResultsPanel();
    }

    private void showResultsPanel(String baseName) {
        String colorFileName = baseName + "_COLOR.pdf";
        String bwFileName = baseName + "_BN.pdf";
        
        colorFileNameLabel.setText(colorFileName);
        bwFileNameLabel.setText(bwFileName);
        
        checkAndEnableOpenButtons();
        resultsPanel.setVisible(true);
        resultsPanel.setManaged(true);
    }

    private void hideResultsPanel() {
        resultsPanel.setVisible(false);
        resultsPanel.setManaged(false);
        openColorButton.setDisable(true);
        openBwButton.setDisable(true);
        colorFileNameLabel.setText("—");
        bwFileNameLabel.setText("—");
    }

    private void checkAndEnableOpenButtons() {
        if (lastExportDir != null && lastExportDir.isDirectory()) {
            File colorFile = new File(lastExportDir, colorFileNameLabel.getText());
            File bwFile = new File(lastExportDir, bwFileNameLabel.getText());
            openColorButton.setDisable(!colorFile.exists());
            openBwButton.setDisable(!bwFile.exists());
        } else {
            openColorButton.setDisable(true);
            openBwButton.setDisable(true);
        }
    }

    @FXML
    private void handleOpenColor() {
        openPdfInBrowser(colorFileNameLabel.getText());
    }

    @FXML
    private void handleOpenBw() {
        openPdfInBrowser(bwFileNameLabel.getText());
    }

    private void openPdfInBrowser(String fileName) {
        if (lastExportDir == null || fileName.equals("—")) return;
        
        File file = new File(lastExportDir, fileName);
        if (!file.exists()) {
            state.setStatus("Archivo no encontrado: " + fileName);
            return;
        }
        
        try {
            Desktop.getDesktop().browse(file.toURI());
            state.setStatus("Abierto en navegador: " + fileName);
        } catch (IOException e) {
            state.setStatus("Error al abrir: " + e.getMessage());
        }
    }

    private void updatePriceDisplay() {
        int bwPages = parseIntSafe(bwLabel.getText());
        int colorPages = parseIntSafe(colorLabel.getText());
        updatePriceDisplay(bwPages, colorPages);
    }

    private void updatePriceDisplay(int bwPages, int colorPages) {
        double bwPrice = priceStorage.loadBwPrice();
        double colorPrice = priceStorage.loadColorBasePrice(); // Use base price as single color price
        double extras = parseDoubleSafe(extrasField.getText());
        
        double bwTotal = pricingCalculator.calculateBwTotal(bwPages, bwPrice);
        double colorTotal = pricingCalculator.calculateColorTotal(colorPages, colorPrice);
        double total = bwTotal + colorTotal + extras;
        
        totalBwPriceLabel.setText(formatPrice(bwTotal));
        totalColorPriceLabel.setText(formatPrice(colorTotal));
        totalPriceLabel.setText(formatPrice(total));
    }
}
