-- ============ TABLES ============
CREATE TABLE IF NOT EXISTS categories (
  id   INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE
);

-- amount is signed: income > 0, expense < 0
CREATE TABLE IF NOT EXISTS transactions (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  date        TEXT NOT NULL CHECK (date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
  description TEXT NOT NULL,
  amount      REAL NOT NULL CHECK (amount <> 0),
  category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS budgets (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  category_id  INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
  month        TEXT NOT NULL CHECK (month GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]'),
  limit_amount REAL NOT NULL CHECK (limit_amount > 0),
  UNIQUE (category_id, month)
);

-- ============ INDEXES ============
-- also makes CSV re-imports idempotent (INSERT OR IGNORE skips duplicates)
CREATE UNIQUE INDEX IF NOT EXISTS ux_txn_dedupe ON transactions(date, description, amount);
CREATE INDEX IF NOT EXISTS ix_txn_category ON transactions(category_id);

-- ============ VIEWS ============
-- Monthly totals + month-over-month change + rolling average + cumulative net.
-- CTE feeds window functions (LAG, moving AVG, running SUM).
CREATE VIEW IF NOT EXISTS v_monthly_summary AS
WITH monthly AS (
  SELECT substr(date, 1, 7) AS month,
         SUM(CASE WHEN amount > 0 THEN  amount ELSE 0 END) AS income,
         SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END) AS expense
  FROM transactions
  GROUP BY substr(date, 1, 7)
)
SELECT month,
       income,
       expense,
       income - expense AS net,
       expense - LAG(expense) OVER (ORDER BY month) AS expense_change,
       ROUND(100.0 * (expense - LAG(expense) OVER (ORDER BY month))
             / NULLIF(LAG(expense) OVER (ORDER BY month), 0), 1) AS expense_change_pct,
       ROUND(AVG(expense) OVER (ORDER BY month ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 2) AS expense_3m_avg,
       SUM(income - expense) OVER (ORDER BY month) AS cumulative_net
FROM monthly;

-- Budget vs actual per category per month
CREATE VIEW IF NOT EXISTS v_budget_status AS
SELECT b.id,
       c.name AS category,
       b.month,
       b.limit_amount,
       COALESCE(SUM(-t.amount), 0) AS spent,
       ROUND(100.0 * COALESCE(SUM(-t.amount), 0) / b.limit_amount, 1) AS pct_used
FROM budgets b
JOIN categories c ON c.id = b.category_id
LEFT JOIN transactions t
       ON t.category_id = b.category_id
      AND substr(t.date, 1, 7) = b.month
      AND t.amount < 0
GROUP BY b.id;
