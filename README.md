# Personal Finance Analytics (JavaFX + SQLite)

Desktop app to track transactions, set budgets, and analyse spending with SQL window functions.

## Run Application

### Option 1: Quick Launch Script (Recommended)
Double-click `run.bat` or execute in terminal:

```cmd
run.bat
```
or with PowerShell:
```powershell
.\run.ps1
```

### Option 2: Maven
Requires JDK 21+ and Maven.

```bash
mvn javafx:run
```

First launch creates `finance.db` and seeds ~6 months of demo data. Delete the file to reset.
Drag `sample-import.csv` onto the window to test CSV import (it includes one bad row and one `dd/MM/yyyy` date on purpose).

---

## Run Tests
Automated suite tests DAOs, SQL window functions, duplicate constraints, and CSV import/export:

```cmd
run-tests.bat
```
or with PowerShell:
```powershell
.\run-tests.ps1
```

---

## Build
To recompile all classes:

```cmd
build.bat
```
or
```powershell
.\build.ps1
```

---

## Features
- **Transactions:** 
  - Live filters (text search, category dropdown, income/expense type, date range picker)
  - Sortable table with running balance computed via SQL window functions
  - Add & delete transactions with duplicate protection
  - **CSV Import:** Via button or direct drag-and-drop onto the window
  - **CSV Export:** Export filtered transactions view directly to `.csv`
- **Dashboard:** KPI summary cards, category spend PieChart, income/expense/3-month moving average LineChart, and month-over-month table
- **Budgets:** Budget vs actual with colour-coded progress bars and upsert limit management

---

## SQL Used (`schema.sql`, `FinanceDao.java`)
| Concept | Where |
|---|---|
| Window `SUM() OVER` running balance | `transactionsWithBalance()` |
| `LAG()` month-over-month change, moving `AVG()` (`ROWS BETWEEN 2 PRECEDING`) | `v_monthly_summary` |
| CTE feeding window functions | `v_monthly_summary` |
| `GROUP BY` + `HAVING` | `spendByCategory()` (slider on dashboard) |
| Views, `LEFT JOIN` aggregate | `v_budget_status` |
| UPSERT `ON CONFLICT DO UPDATE` | `upsertBudget()` |
| Unique index for idempotent imports, CHECK + FK constraints | `schema.sql` |
| Single transaction with rollback | `CsvImporter` |

---

## Architecture
```
ui (JavaFX tabs) -> Async (single DB worker thread) -> FinanceDao -> SQLite
      ^                                                    |
      +---------------- EventBus (TRANSACTIONS_CHANGED) ---+
```

- Writes publish an event; every tab reloads itself asynchronously (tabs never call each other).
- All DB calls run on a dedicated background thread: UI never freezes and SQLite writes never collide.
- All queries use `PreparedStatement`.
