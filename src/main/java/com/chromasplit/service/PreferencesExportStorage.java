package com.chromasplit.service;

import java.util.prefs.Preferences;

public class PreferencesExportStorage implements ExportStorage {
    private static final String KEY_LAST_EXPORT_DIR = "last_export_dir";

    private final Preferences prefs = Preferences.userNodeForPackage(PreferencesExportStorage.class);

    @Override
    public String loadLastExportDir() {
        return prefs.get(KEY_LAST_EXPORT_DIR, "");
    }

    @Override
    public void saveLastExportDir(String path) {
        prefs.put(KEY_LAST_EXPORT_DIR, path != null ? path : "");
    }
}
