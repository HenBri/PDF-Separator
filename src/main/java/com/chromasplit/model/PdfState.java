package com.chromasplit.model;

import javafx.beans.property.*;

public class PdfState {
    private final StringProperty status = new SimpleStringProperty("Esperando archivo...");
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final BooleanProperty complete = new SimpleBooleanProperty(false);
    private final StringProperty fileName = new SimpleStringProperty("");
    private final StringProperty outputDir = new SimpleStringProperty("");

    public StringProperty statusProperty() { return status; }
    public DoubleProperty progressProperty() { return progress; }
    public BooleanProperty runningProperty() { return running; }
    public BooleanProperty completeProperty() { return complete; }
    public StringProperty fileNameProperty() { return fileName; }
    public StringProperty outputDirProperty() { return outputDir; }

    public String getStatus() { return status.get(); }
    public void setStatus(String s) { status.set(s); }
    public double getProgress() { return progress.get(); }
    public void setProgress(double p) { progress.set(p); }
    public boolean isRunning() { return running.get(); }
    public void setRunning(boolean r) { running.set(r); }
    public boolean isComplete() { return complete.get(); }
    public void setComplete(boolean c) { complete.set(c); }
    public String getFileName() { return fileName.get(); }
    public void setFileName(String f) { fileName.set(f); }
    public String getOutputDir() { return outputDir.get(); }
    public void setOutputDir(String d) { outputDir.set(d); }

    public void reset() {
        setStatus("Esperando archivo...");
        setProgress(0);
        setRunning(false);
        setComplete(false);
        setFileName("");
        setOutputDir("");
    }
}