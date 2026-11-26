# 💰 Personal Finance Analytics

A desktop-based Personal Finance Management and Analytics application built using **Java 21, JavaFX, JDBC, and MySQL 8**.

The application helps users manage income, track expenses, monitor monthly budgets, and analyse financial trends using advanced SQL operations.

## 📌 Project Overview

Personal Finance Analytics transforms everyday financial transactions into meaningful insights through an interactive desktop dashboard.

Instead of performing all calculations in Java, the application uses SQL queries, aggregate functions, window functions, and database views to generate financial analytics.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Core application logic |
| JavaFX 21 | Desktop GUI |
| MySQL 8 | Database management |
| JDBC | Database connectivity |
| Maven | Dependency management |
| JUnit 5 | Unit testing |
| CSS | UI styling |

## ✨ Key Features

### 1. Transaction Management
- Add and delete income and expense transactions.
- Filter transactions by category, date, and type.
- Search transactions by description.
- Import transactions through CSV files.
- Prevent duplicate transaction entries.

### 2. Financial Dashboard
- Monthly income and expense summaries.
- Net savings calculations.
- Savings rate analysis.
- Category-wise spending visualization.
- Monthly financial trend charts.

### 3. Budget Management
- Set monthly budgets for individual categories.
- Track actual spending against budget limits.
- Monitor budget utilization.
- Identify overspending categories.

### 4. Advanced SQL Analytics
The project demonstrates practical SQL concepts:

- INNER JOIN and LEFT JOIN
- GROUP BY and HAVING
- Common Table Expressions (CTEs)
- Window Functions
- LAG() for month-over-month analysis
- Moving averages
- Running balances
- Aggregate functions
- Database Views
- UPSERT operations
- Transaction management and rollback

## 🏗️ Project Architecture

```text
             JavaFX Application
                     |
          -----------------------
          |          |          |
     Transactions Dashboard  Budgets
          |          |          |
          -----------------------
                     |
                EventBus
                     |
             Async Worker
                     |
                 FinanceDAO
                     |
                  JDBC
                     |
                 MySQL 8
```

## 📂 Project Structure

```text
Finance_analytics/
│
├── database/
│   └── schema.sql
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/financeanalytics/
│   │   └── resources/
│   │
│   └── test/
│
├── pom.xml
├── README.md
├── sample-import.csv
└── .gitignore
```

## ⚙️ Installation and Setup

### Prerequisites

- JDK 21
- MySQL Server 8.0+
- Maven
- Visual Studio Code

### Step 1: Clone the repository

```bash
git clone https://github.com/Amreen1902/Finance_analytics-.git
```

### Step 2: Open the project

```bash
cd Finance_analytics-
```

### Step 3: Configure MySQL

Execute the SQL script:

```text
database/schema.sql
```

This creates the database and required tables and views.

### Step 4: Configure database credentials

Set your environment variables:

```powershell
$env:FINANCE_DB_USER="root"
$env:FINANCE_DB_PASSWORD="your_password"
```

### Step 5: Run tests

```bash
mvn clean test
```

### Step 6: Launch the application

```bash
mvn javafx:run
```

## 📊 Database Design

The application uses three primary tables:

- `categories` – Stores transaction categories.
- `transactions` – Stores income and expense records.
- `budgets` – Stores monthly category budgets.

It also uses two database views:

- `v_monthly_summary`
- `v_budget_status`

## 🔮 Future Improvements

- Interactive financial reports.
- Export analytics to PDF and Excel.
- User authentication and multiple accounts.
- Improved financial forecasting.
- Database integration testing.
- More advanced spending predictions.


---

⭐ If you find this project interesting, consider giving it a star!
