package com.chromasplit.service;

public interface ExportStorage {
    String loadLastExportDir();
    void saveLastExportDir(String path);
}
