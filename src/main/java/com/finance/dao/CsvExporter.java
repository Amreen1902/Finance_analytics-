package com.finance.dao;

import com.finance.model.Models.Txn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Exports transaction list to CSV format: Date,Description,Amount,Category,Running Balance
 */
public final class CsvExporter {
    private CsvExporter() {}

    public static void exportToFile(File file, List<Txn> txns) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            w.write("Date,Description,Amount,Category,Running Balance\n");
            for (Txn t : txns) {
                w.write(String.format("%s,%s,%.2f,%s,%.2f\n",
                        t.date(),
                        escapeCsv(t.description()),
                        t.amount(),
                        escapeCsv(t.category()),
                        t.balance()));
            }
        }
    }

    static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

