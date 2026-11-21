package com.finance.model;

import java.time.LocalDate;

/** Immutable data carriers shared by DAO and UI. */
public final class Models {
    private Models() {}

    public record Category(int id, String name) {
        @Override public String toString() { return name; }
    }

    /** balance = running balance after this row (window function). */
    public record Txn(int id, LocalDate date, String description, double amount,
                      int categoryId, String category, double balance) {}

    public record CategorySpend(String category, double total) {}

    /** expenseChange / expenseChangePct are null for the first month. */
    public record MonthRow(String month, double income, double expense, double net,
                           Double expenseChange, Double expenseChangePct,
                           double expense3mAvg, double cumulativeNet) {}

    public record BudgetRow(int id, String category, String month,
                            double limit, double spent, double pctUsed) {}
}
