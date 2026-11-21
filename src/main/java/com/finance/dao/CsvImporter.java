package com.finance.dao;

import com.finance.db.Db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports CSV: date,description,amount[,category]
 * Negative amount = expense. Runs in ONE transaction (all-or-nothing on DB errors);
 * bad rows are reported, duplicates are skipped via the unique index.
 */
public final class CsvImporter {
    private CsvImporter() {}

    public record Result(int inserted, int skipped, List<String> errors) {}

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")};

    public static Result importFile(Path file) throws IOException, SQLException {
        List<String> lines = Files.readAllLines(file);
        int inserted = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement insCat = c.prepareStatement("INSERT OR IGNORE INTO categories(name) VALUES (?)");
                 PreparedStatement getCat = c.prepareStatement("SELECT id FROM categories WHERE name = ?");
                 PreparedStatement insTxn = c.prepareStatement(
                         "INSERT OR IGNORE INTO transactions(date, description, amount, category_id) VALUES (?,?,?,?)")) {

                Map<String, Integer> cache = new HashMap<>();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty() || (i == 0 && line.toLowerCase().startsWith("date"))) continue;
                    try {
                        String[] f = split(line);
                        if (f.length < 3) throw new IllegalArgumentException("expected date,description,amount[,category]");
                        LocalDate date = parseDate(f[0].trim());
                        double amount = parseAmount(f[2]);
                        if (amount == 0) throw new IllegalArgumentException("amount cannot be zero");
                        String desc = f[1].trim();
                        if (desc.isEmpty()) throw new IllegalArgumentException("description is empty");
                        String cat = f.length > 3 && !f[3].isBlank() ? f[3].trim() : "Uncategorized";

                        int catId = categoryId(insCat, getCat, cache, cat);
                        insTxn.setString(1, date.toString());
                        insTxn.setString(2, desc);
                        insTxn.setDouble(3, amount);
                        insTxn.setInt(4, catId);
                        if (insTxn.executeUpdate() == 1) inserted++; else skipped++;
                    } catch (IllegalArgumentException ex) {
                        errors.add("Line " + (i + 1) + ": " + ex.getMessage());
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
        return new Result(inserted, skipped, errors);
    }

    private static int categoryId(PreparedStatement insCat, PreparedStatement getCat,
                                  Map<String, Integer> cache, String name) throws SQLException {
        Integer cached = cache.get(name);
        if (cached != null) return cached;
        insCat.setString(1, name);
        insCat.executeUpdate();
        getCat.setString(1, name);
        try (ResultSet rs = getCat.executeQuery()) {
            rs.next();
            int id = rs.getInt(1);
            cache.put(name, id);
            return id;
        }
    }

    private static LocalDate parseDate(String s) {
        for (DateTimeFormatter f : FORMATS) {
            try { return LocalDate.parse(s, f); } catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("unrecognised date '" + s + "' (use yyyy-MM-dd or dd/MM/yyyy)");
    }

    private static double parseAmount(String raw) {
        String s = raw.trim();
        boolean bracketNegative = s.startsWith("(") && s.endsWith(")");
        s = s.replaceAll("[^0-9.\\-]", "");
        try {
            double v = Double.parseDouble(s);
            return bracketNegative ? -Math.abs(v) : v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid amount '" + raw.trim() + "'");
        }
    }

    /** Quote-aware CSV split ("a, b",c and "" escapes). */
    static String[] split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else sb.append(ch);
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }
}
