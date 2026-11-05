package com.chromasplit;

import com.chromasplit.service.*;
import com.chromasplit.ui.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChromasplitApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        PriceStorage priceStorage = new PreferencesPriceStorage();
        ExportStorage exportStorage = new PreferencesExportStorage();
        ChromaAnalyzer analyzer = new ChromaAnalyzer();
        PdfSplitter splitter = new PdfSplitter(analyzer);
        PdfService pdfService = new PdfService(splitter);

        var loader = new FXMLLoader(getClass().getResource("/com/chromasplit/ui/main.fxml"));
        loader.setControllerFactory(clazz -> {
            if (clazz == MainController.class) {
                return new MainController(priceStorage, exportStorage, pdfService);
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        var root = loader.<javafx.scene.layout.BorderPane>load();
        var scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(getClass().getResource("/com/chromasplit/css/theme.css").toExternalForm());

        primaryStage.setTitle("DskV2 - v2.0.0");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
