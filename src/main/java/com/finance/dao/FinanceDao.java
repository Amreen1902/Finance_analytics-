package com.finance.dao;

import com.finance.db.Db;
import com.finance.model.Models.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL lives here. Every query uses PreparedStatement.
 * Methods block, so the UI must call them through Async.run(...).
 */
public final class FinanceDao {

    public List<Category> categories() {
        List<Category> out = new ArrayList<>();
        try (Connection c = Db.connect(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM categories ORDER BY name")) {
            while (rs.next()) out.add(new Category(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    /** Window function: running balance across ALL transactions, newest first for display. */
    public List<Txn> transactionsWithBalance() {
        String sql = """
            SELECT t.id, t.date, t.description, t.amount, t.category_id, c.name,
                   SUM(t.amount) OVER (ORDER BY t.date, t.id
                                       ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            ORDER BY t.date DESC, t.id DESC
            """;
        List<Txn> out = new ArrayList<>();
        try (Connection c = Db.connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new Txn(rs.getInt(1), LocalDate.parse(rs.getString(2)), rs.getString(3),
                        rs.getDouble(4), rs.getInt(5), rs.getString(6), rs.getDouble(7)));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    public void add(LocalDate date, String description, double amount, int categoryId) {
        String sql = "INSERT INTO transactions(date, description, amount, category_id) VALUES (?,?,?,?)";
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setString(2, description);
            ps.setDouble(3, amount);
            ps.setInt(4, categoryId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
                throw new IllegalStateException("A transaction with the same date, description and amount already exists.");
            throw fail(e);
        }
    }

    public void delete(int id) {
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    /** GROUP BY + HAVING: spend per category for a month, hiding categories below minTotal. */
    public List<CategorySpend> spendByCategory(String month, double minTotal) {
        String sql = """
            SELECT c.name, SUM(-t.amount) AS total
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.amount < 0 AND substr(t.date, 1, 7) = ?
            GROUP BY c.id, c.name
            HAVING SUM(-t.amount) >= ?
            ORDER BY total DESC
            """;
        List<CategorySpend> out = new ArrayList<>();
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, month);
            ps.setDouble(2, minTotal);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new CategorySpend(rs.getString(1), rs.getDouble(2)));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    /** Reads the v_monthly_summary view (CTE + LAG + moving average + running total). */
    public List<MonthRow> monthlySummary() {
        List<MonthRow> out = new ArrayList<>();
        try (Connection c = Db.connect(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM v_monthly_summary ORDER BY month")) {
            while (rs.next()) {
                double chg = rs.getDouble("expense_change");
                Double change = rs.wasNull() ? null : chg;
                double pct = rs.getDouble("expense_change_pct");
                Double changePct = rs.wasNull() ? null : pct;
                out.add(new MonthRow(rs.getString("month"), rs.getDouble("income"), rs.getDouble("expense"),
                        rs.getDouble("net"), change, changePct,
                        rs.getDouble("expense_3m_avg"), rs.getDouble("cumulative_net")));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    public List<String> months() {
        List<String> out = new ArrayList<>();
        try (Connection c = Db.connect(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT DISTINCT substr(date, 1, 7) FROM transactions ORDER BY 1 DESC")) {
            while (rs.next()) out.add(rs.getString(1));
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    public List<BudgetRow> budgetStatus(String month) {
        List<BudgetRow> out = new ArrayList<>();
        String sql = "SELECT id, category, month, limit_amount, spent, pct_used FROM v_budget_status WHERE month = ? ORDER BY pct_used DESC";
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new BudgetRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDouble(4), rs.getDouble(5), rs.getDouble(6)));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    /** UPSERT: insert a budget or update the limit if (category, month) already exists. */
    public void upsertBudget(int categoryId, String month, double limit) {
        String sql = """
            INSERT INTO budgets(category_id, month, limit_amount) VALUES (?,?,?)
            ON CONFLICT(category_id, month) DO UPDATE SET limit_amount = excluded.limit_amount
            """;
        try (Connection c = Db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, month);
            ps.setDouble(3, limit);
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    private static IllegalStateException fail(SQLException e) {
        return new IllegalStateException(e.getMessage(), e);
    }
}
