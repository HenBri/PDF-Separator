module com.chromasplit {
    requires java.desktop;
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.pdfbox;

    exports com.chromasplit;
    opens com.chromasplit.ui.controller to javafx.fxml;
}
