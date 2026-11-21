package com.finance.ui;

import com.finance.dao.CsvImporter;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Shared by the "Import CSV" button and drag-and-drop. */
public final class ImportAction {
    private ImportAction() {}

    public static void run(List<File> files) {
        List<File> csvs = files.stream().filter(f -> f.getName().toLowerCase().endsWith(".csv")).toList();
        if (csvs.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Drop one or more .csv files.", ButtonType.OK).show();
            return;
        }
        Async.run(() -> {
            int inserted = 0, skipped = 0;
            List<String> errors = new ArrayList<>();
            for (File f : csvs) {
                CsvImporter.Result r = CsvImporter.importFile(f.toPath());
                inserted += r.inserted();
                skipped += r.skipped();
                r.errors().forEach(e -> errors.add(f.getName() + " - " + e));
            }
            return new CsvImporter.Result(inserted, skipped, errors);
        }, r -> {
            EventBus.publish(EventBus.Topic.TRANSACTIONS_CHANGED);
            StringBuilder sb = new StringBuilder()
                    .append(r.inserted()).append(" imported, ")
                    .append(r.skipped()).append(" duplicates skipped, ")
                    .append(r.errors().size()).append(" bad rows.");
            r.errors().stream().limit(8).forEach(e -> sb.append("\n• ").append(e));
            new Alert(r.errors().isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                    sb.toString(), ButtonType.OK).show();
        });
    }
}
