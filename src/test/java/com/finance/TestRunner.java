package com.finance;

import com.finance.dao.CsvExporter;
import com.finance.dao.CsvImporter;
import com.finance.dao.FinanceDao;
import com.finance.db.Db;
import com.finance.model.Models.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Self-contained test suite that exercises DAOs, SQL window functions, CSV import, and CSV export
 * using an isolated temporary SQLite database.
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running Personal Finance Analytics Test Suite");
        System.out.println("==================================================");

        Path tempDb = null;
        try {
            tempDb = Files.createTempFile("finance-test-", ".db");
            Db.setUrl("jdbc:sqlite:" + tempDb.toAbsolutePath().toString().replace('\\', '/'));
            Db.init();
            com.finance.db.Seed.ifEmpty();

            testCategoriesAndAdd();
            testWindowFunctionsRunningBalance();
            testDuplicateDetection();
            testSpendByCategoryHaving();
            testMonthlySummaryWindowFunctions();
            testBudgetsUpsert();
            testCsvImporter();
            testCsvExporter();

        } catch (Throwable t) {
            System.err.println("Fatal setup error: " + t.getMessage());
            t.printStackTrace();
            failed++;
        } finally {
            Db.resetUrl();
            if (tempDb != null) {
                try { Files.deleteIfExists(tempDb); } catch (Exception ignored) {}
            }
        }

        System.out.println("--------------------------------------------------");
        System.out.printf("Results: %d passed, %d failed.%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    @FunctionalInterface
    public interface TestCase {
        void run() throws Throwable;
    }

    private static void check(String testName, TestCase test) {
        try {
            test.run();
            System.out.println("[PASS] " + testName);
            passed++;
        } catch (Throwable t) {
            System.out.println("[FAIL] " + testName + " -> " + t.getMessage());
            t.printStackTrace(System.out);
            failed++;
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " (Expected: " + expected + ", Actual: " + actual + ")");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void testCategoriesAndAdd() {
        check("Categories & Add Transaction", () -> {
            FinanceDao dao = new FinanceDao();
            List<Category> cats = dao.categories();
            assertTrue(!cats.isEmpty(), "Categories table should be populated");

            int catId = cats.get(0).id();
            dao.add(LocalDate.now(), "Unique Salary Payment Test", 50000.0, catId);

            List<Txn> txns = dao.transactionsWithBalance();
            assertTrue(txns.stream().anyMatch(t -> t.description().equals("Unique Salary Payment Test")),
                    "Should find the added transaction");
        });
    }

    private static void testWindowFunctionsRunningBalance() {
        check("Window Function SUM() OVER running balance", () -> {
            FinanceDao dao = new FinanceDao();
            List<Category> cats = dao.categories();
            int catId = cats.get(0).id();

            LocalDate testDate = LocalDate.now();
            dao.add(testDate, "Unique Test Running Balance 1", 1000.0, catId);
            dao.add(testDate, "Unique Test Running Balance 2", -200.0, catId);

            List<Txn> list = dao.transactionsWithBalance();
            Txn t2 = list.stream().filter(t -> t.description().equals("Unique Test Running Balance 2")).findFirst().orElseThrow();
            Txn t1 = list.stream().filter(t -> t.description().equals("Unique Test Running Balance 1")).findFirst().orElseThrow();
            assertEquals(t1.balance() - 200.0, t2.balance(), "Running balance difference should match amount");
        });
    }

    private static void testDuplicateDetection() {
        check("Duplicate Transaction Protection", () -> {
            FinanceDao dao = new FinanceDao();
            List<Category> cats = dao.categories();
            int catId = cats.get(0).id();

            LocalDate testDate = LocalDate.now();
            dao.add(testDate, "Unique Duplicate Check Item", -45.0, catId);

            boolean caught = false;
            try {
                dao.add(testDate, "Unique Duplicate Check Item", -45.0, catId);
            } catch (IllegalStateException ex) {
                caught = true;
            }
            assertTrue(caught, "Duplicate date+description+amount should throw IllegalStateException");
        });
    }

    private static void testSpendByCategoryHaving() {
        check("GROUP BY + HAVING spendByCategory()", () -> {
            FinanceDao dao = new FinanceDao();
            List<String> months = dao.months();
            assertTrue(!months.isEmpty(), "Should have months");
            String month = months.get(0);
            List<CategorySpend> spend = dao.spendByCategory(month, 500.0);
            assertTrue(spend.stream().allMatch(s -> s.total() >= 500.0),
                    "All categories returned should have spend >= minTotal");
        });
    }

    private static void testMonthlySummaryWindowFunctions() {
        check("v_monthly_summary window functions (LAG, moving AVG, cumulative)", () -> {
            FinanceDao dao = new FinanceDao();
            List<MonthRow> rows = dao.monthlySummary();
            assertTrue(!rows.isEmpty(), "Monthly summary should contain data");
            MonthRow latest = rows.get(rows.size() - 1);
            assertTrue(latest.income() > 0 || latest.expense() > 0, "Latest month has activity");
            assertTrue(latest.cumulativeNet() != 0, "Cumulative net is calculated");
        });
    }

    private static void testBudgetsUpsert() {
        check("UPSERT on budgets table", () -> {
            FinanceDao dao = new FinanceDao();
            List<Category> cats = dao.categories();
            int catId = cats.get(0).id();
            String month = dao.months().get(0);

            dao.upsertBudget(catId, month, 5000.0);
            List<BudgetRow> b1 = dao.budgetStatus(month);
            assertTrue(b1.stream().anyMatch(b -> b.limit() == 5000.0), "Initial budget limit 5000");

            // Update limit
            dao.upsertBudget(catId, month, 7500.0);
            List<BudgetRow> b2 = dao.budgetStatus(month);
            assertTrue(b2.stream().anyMatch(b -> b.limit() == 7500.0), "Upserted budget limit 7500");
        });
    }

    private static void testCsvImporter() {
        check("CsvImporter format support and error handling", () -> {
            String csvContent = """
                    Date,Description,Amount,Category
                    2026-02-01,Freelance Project,15000,Salary
                    02/02/2026,"Dinner, with friends",-1250,Dining
                    invalid-date,Bad Item,-50,Dining
                    2026-02-01,Freelance Project,15000,Salary
                    """;
            Path tempCsv = Files.createTempFile("test-import-", ".csv");
            try {
                Files.writeString(tempCsv, csvContent);
                CsvImporter.Result res = CsvImporter.importFile(tempCsv);
                assertEquals(2, res.inserted(), "Should insert 2 valid rows");
                assertEquals(1, res.skipped(), "Should skip 1 duplicate row");
                assertEquals(1, res.errors().size(), "Should record 1 bad date format error");
            } finally {
                Files.deleteIfExists(tempCsv);
            }
        });
    }

    private static void testCsvExporter() {
        check("CsvExporter exportToFile with comma quoting", () -> {
            Txn t = new Txn(1, LocalDate.of(2026, 2, 5), "Books, Stationery & Pens", -850.0, 1, "Shopping", 47350.0);
            Path tempCsv = Files.createTempFile("test-export-", ".csv");
            try {
                CsvExporter.exportToFile(tempCsv.toFile(), List.of(t));
                List<String> lines = Files.readAllLines(tempCsv);
                assertTrue(lines.size() >= 2, "Header + 1 data line");
                assertTrue(lines.get(1).contains("\"Books, Stationery & Pens\""), "Description with comma must be quoted");
            } finally {
                Files.deleteIfExists(tempCsv);
            }
        });
    }
}
