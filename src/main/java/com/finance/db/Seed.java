package com.finance.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Generates ~6 months of realistic demo data on first run so charts are never empty. */
public final class Seed {
    private Seed() {}

    private static final String[] CATEGORIES = {
            "Salary", "Rent", "Utilities", "Groceries", "Dining",
            "Transport", "Entertainment", "Shopping", "Health"};

    public static void ifEmpty() {
        try (Connection c = Db.connect()) {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM transactions")) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
            c.setAutoCommit(false);
            Map<String, Integer> ids = new HashMap<>();
            try (PreparedStatement insCat = c.prepareStatement("INSERT OR IGNORE INTO categories(name) VALUES (?)");
                 PreparedStatement getCat = c.prepareStatement("SELECT id FROM categories WHERE name = ?");
                 PreparedStatement ins = c.prepareStatement(
                         "INSERT OR IGNORE INTO transactions(date, description, amount, category_id) VALUES (?,?,?,?)");
                 PreparedStatement bud = c.prepareStatement(
                         "INSERT OR IGNORE INTO budgets(category_id, month, limit_amount) VALUES (?,?,?)")) {

                for (String name : CATEGORIES) {
                    insCat.setString(1, name);
                    insCat.executeUpdate();
                    getCat.setString(1, name);
                    try (ResultSet rs = getCat.executeQuery()) { rs.next(); ids.put(name, rs.getInt(1)); }
                }

                Random r = new Random(42);
                LocalDate today = LocalDate.now();
                for (int i = 5; i >= 0; i--) {
                    YearMonth ym = YearMonth.now().minusMonths(i);
                    add(ins, ym, 1, "Monthly salary", 60000, ids.get("Salary"), today);
                    add(ins, ym, 3, "House rent", -15000, ids.get("Rent"), today);
                    add(ins, ym, 7, "Electricity & internet", -(1800 + r.nextInt(900)), ids.get("Utilities"), today);
                    for (int k = 0; k < 6 + r.nextInt(3); k++)
                        add(ins, ym, 1 + r.nextInt(28), "Groceries #" + (k + 1), -(300 + r.nextInt(1200)), ids.get("Groceries"), today);
                    for (int k = 0; k < 4 + r.nextInt(3); k++)
                        add(ins, ym, 1 + r.nextInt(28), "Restaurant #" + (k + 1), -(250 + r.nextInt(900)), ids.get("Dining"), today);
                    for (int k = 0; k < 5 + r.nextInt(4); k++)
                        add(ins, ym, 1 + r.nextInt(28), "Cab / fuel #" + (k + 1), -(80 + r.nextInt(500)), ids.get("Transport"), today);
                    for (int k = 0; k < 2 + r.nextInt(2); k++)
                        add(ins, ym, 1 + r.nextInt(28), "Movies / OTT #" + (k + 1), -(200 + r.nextInt(700)), ids.get("Entertainment"), today);
                    for (int k = 0; k < 1 + r.nextInt(3); k++)
                        add(ins, ym, 1 + r.nextInt(28), "Shopping #" + (k + 1), -(500 + r.nextInt(2500)), ids.get("Shopping"), today);
                    if (r.nextBoolean())
                        add(ins, ym, 1 + r.nextInt(28), "Pharmacy / clinic", -(300 + r.nextInt(1500)), ids.get("Health"), today);

                    String m = ym.toString();
                    budget(bud, ids.get("Groceries"), m, 8000);
                    budget(bud, ids.get("Dining"), m, 4000);
                    budget(bud, ids.get("Transport"), m, 3000);
                    budget(bud, ids.get("Entertainment"), m, 2000);
                    budget(bud, ids.get("Shopping"), m, 4000);
                }
            }
            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Seeding failed: " + e.getMessage(), e);
        }
    }

    private static void add(PreparedStatement ps, YearMonth ym, int day, String desc, double amt,
                            int catId, LocalDate today) throws SQLException {
        LocalDate d = ym.atDay(Math.min(day, ym.lengthOfMonth()));
        if (d.isAfter(today)) return;
        ps.setString(1, d.toString());
        ps.setString(2, desc);
        ps.setDouble(3, amt);
        ps.setInt(4, catId);
        ps.executeUpdate();
    }

    private static void budget(PreparedStatement ps, int catId, String month, double limit) throws SQLException {
        ps.setInt(1, catId);
        ps.setString(2, month);
        ps.setDouble(3, limit);
        ps.executeUpdate();
    }
}
