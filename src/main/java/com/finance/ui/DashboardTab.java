package com.finance.ui;

import com.finance.dao.FinanceDao;
import com.finance.model.Models.CategorySpend;
import com.finance.model.Models.MonthRow;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** KPI cards, PieChart (HAVING-filtered category spend), LineChart (trend) and MoM table. */
public class DashboardTab extends VBox {
    private final FinanceDao dao;

    private final ComboBox<String> month = new ComboBox<>();
    private final Slider minSpend = new Slider(0, 5000, 0);
    private final Label minSpendLabel = new Label("Hide categories below ₹0");
    private final Label kpiIncome = kpi();
    private final Label kpiExpense = kpi();
    private final Label kpiNet = kpi();
    private final Label kpiRate = kpi();

    private final PieChart pie = new PieChart();
    private final LineChart<String, Number> line = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final TableView<MonthRow> table = new TableView<>();

    private List<MonthRow> rows = List.of();
    private boolean updating = false;

    public DashboardTab(FinanceDao dao) {
        this.dao = dao;
        setSpacing(12);
        setPadding(new Insets(12));

        minSpend.setBlockIncrement(250);
        minSpend.setPrefWidth(200);
        minSpend.valueProperty().addListener((o, a, b) -> {
            minSpendLabel.setText(String.format("Hide categories below ₹%,.0f", b.doubleValue()));
            if (!updating) loadPie();
        });
        month.valueProperty().addListener((o, a, b) -> { if (!updating) updateMonthViews(); });

        HBox controls = new HBox(12, new Label("Month:"), month, minSpendLabel, minSpend);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox kpis = new HBox(12,
                card("Income", kpiIncome), card("Expense", kpiExpense),
                card("Net", kpiNet), card("Savings rate", kpiRate));

        pie.setLegendVisible(true);
        pie.setLabelsVisible(false);
        pie.setPrefHeight(300);
        line.setAnimated(false);
        line.setCreateSymbols(true);
        line.setPrefHeight(300);
        line.setTitle("Monthly trend");
        line.getYAxis().setLabel("₹");
        HBox.setHgrow(pie, Priority.ALWAYS);
        HBox.setHgrow(line, Priority.ALWAYS);
        HBox charts = new HBox(12, pie, line);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(controls, kpis, charts, table);

        EventBus.subscribe(EventBus.Topic.TRANSACTIONS_CHANGED, this::refresh);
        refresh();
    }

    private static Label kpi() {
        Label l = new Label("—");
        l.getStyleClass().add("kpi-value");
        return l;
    }

    private static VBox card(String title, Label value) {
        Label t = new Label(title);
        t.getStyleClass().add("kpi-title");
        VBox box = new VBox(4, t, value);
        box.getStyleClass().add("kpi-card");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void buildTable() {
        table.getColumns().addAll(
                col("Month", 90, MonthRow::month),
                col("Income", 110, r -> Fmt.money(r.income())),
                col("Expense", 110, r -> Fmt.money(r.expense())),
                col("Net", 110, r -> Fmt.money(r.net())),
                col("MoM Δ expense", 120, r -> Fmt.signedMoney(r.expenseChange())),
                col("MoM %", 90, r -> Fmt.signedPct(r.expenseChangePct())),
                col("3-mo avg expense", 140, r -> Fmt.money(r.expense3mAvg())),
                col("Cumulative net", 140, r -> Fmt.money(r.cumulativeNet())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(200);
    }

    private TableColumn<MonthRow, String> col(String title, double width, Function<MonthRow, String> f) {
        TableColumn<MonthRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }

    // ---------- data flow ----------

    private record Data(List<String> months, List<MonthRow> rows) {}

    private void refresh() {
        Async.run(() -> new Data(dao.months(), dao.monthlySummary()), d -> {
            rows = d.rows();
            updating = true;
            String keep = month.getValue();
            month.getItems().setAll(d.months());
            month.setValue(d.months().contains(keep) ? keep : (d.months().isEmpty() ? null : d.months().get(0)));
            updating = false;

            table.getItems().setAll(rows);
            fillLine();
            updateMonthViews();
        });
    }

    private void fillLine() {
        XYChart.Series<String, Number> income = new XYChart.Series<>();
        XYChart.Series<String, Number> expense = new XYChart.Series<>();
        XYChart.Series<String, Number> avg = new XYChart.Series<>();
        income.setName("Income");
        expense.setName("Expense");
        avg.setName("3-mo avg expense");
        for (MonthRow r : rows) {
            income.getData().add(new XYChart.Data<>(r.month(), r.income()));
            expense.getData().add(new XYChart.Data<>(r.month(), r.expense()));
            avg.getData().add(new XYChart.Data<>(r.month(), r.expense3mAvg()));
        }
        line.getData().setAll(List.of(income, expense, avg));
    }

    private void updateMonthViews() {
        String m = month.getValue();
        if (m == null) {
            pie.getData().clear();
            kpiIncome.setText("—"); kpiExpense.setText("—"); kpiNet.setText("—"); kpiRate.setText("—");
            return;
        }
        rows.stream().filter(r -> r.month().equals(m)).findFirst().ifPresent(r -> {
            kpiIncome.setText(Fmt.money(r.income()));
            kpiExpense.setText(Fmt.money(r.expense()));
            kpiNet.setText(Fmt.money(r.net()));
            kpiRate.setText(r.income() > 0 ? String.format("%.1f%%", 100.0 * r.net() / r.income()) : "—");
        });
        loadPie();
    }

    private void loadPie() {
        String m = month.getValue();
        if (m == null) return;
        double min = minSpend.getValue();
        Async.run(() -> dao.spendByCategory(m, min), list -> {
            List<PieChart.Data> data = new ArrayList<>();
            for (CategorySpend s : list)
                data.add(new PieChart.Data(String.format("%s (₹%,.0f)", s.category(), s.total()), s.total()));
            pie.setData(FXCollections.observableArrayList(data));
            pie.setTitle("Spending by category — " + m);
        });
    }
}
