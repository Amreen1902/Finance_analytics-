package com.finance.ui;

import com.finance.dao.FinanceDao;
import com.finance.model.Models.BudgetRow;
import com.finance.model.Models.Category;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.List;

/** Budget vs actual with colour-coded progress bars (reads v_budget_status). */
public class BudgetsTab extends VBox {
    private final FinanceDao dao;
    private final ComboBox<String> month = new ComboBox<>();
    private final ComboBox<Category> category = new ComboBox<>();
    private final TextField limit = new TextField();
    private final TableView<BudgetRow> table = new TableView<>();
    private boolean updating = false;

    public BudgetsTab(FinanceDao dao) {
        this.dao = dao;
        setSpacing(12);
        setPadding(new Insets(12));

        category.setPromptText("Category");
        category.setPrefWidth(160);
        limit.setPromptText("Monthly limit (₹)");
        Button set = new Button("Set / update budget");
        set.setOnAction(e -> onSet());

        month.valueProperty().addListener((o, a, b) -> { if (!updating) loadTable(); });

        HBox bar = new HBox(8, new Label("Month:"), month, new Separator(), category, limit, set);
        bar.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(bar, table);

        EventBus.subscribe(EventBus.Topic.TRANSACTIONS_CHANGED, this::refresh);
        EventBus.subscribe(EventBus.Topic.BUDGETS_CHANGED, this::refresh);
        refresh();
    }

    private void buildTable() {
        TableColumn<BudgetRow, String> cat = new TableColumn<>("Category");
        cat.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().category()));

        TableColumn<BudgetRow, String> lim = new TableColumn<>("Limit");
        lim.setCellValueFactory(cd -> new SimpleStringProperty(Fmt.money(cd.getValue().limit())));

        TableColumn<BudgetRow, String> spent = new TableColumn<>("Spent");
        spent.setCellValueFactory(cd -> new SimpleStringProperty(Fmt.money(cd.getValue().spent())));

        TableColumn<BudgetRow, Double> used = new TableColumn<>("Used");
        used.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().pctUsed()));
        used.setCellFactory(c -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            { bar.setPrefWidth(180); }
            @Override protected void updateItem(Double pct, boolean empty) {
                super.updateItem(pct, empty);
                if (empty || pct == null) { setGraphic(null); setText(null); return; }
                bar.setProgress(Math.min(pct / 100.0, 1.0));
                bar.setStyle("-fx-accent:" + (pct >= 100 ? "#c0392b" : pct >= 80 ? "#e67e22" : "#27ae60"));
                setGraphic(bar);
                setText(String.format("%.0f%%", pct));
            }
        });
        used.setPrefWidth(300);

        table.getColumns().addAll(List.of(cat, lim, spent, used));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No budgets for this month. Add one above."));
    }

    private record Data(List<String> months, List<Category> cats) {}

    private void refresh() {
        Async.run(() -> new Data(dao.months(), dao.categories()), d -> {
            updating = true;
            String keep = month.getValue();
            List<String> months = new java.util.ArrayList<>(d.months());
            String current = YearMonth.now().toString();
            if (!months.contains(current)) months.add(0, current);
            month.getItems().setAll(months);
            month.setValue(months.contains(keep) ? keep : months.get(0));
            category.setItems(FXCollections.observableArrayList(d.cats()));
            updating = false;
            loadTable();
        });
    }

    private void loadTable() {
        String m = month.getValue();
        if (m == null) return;
        Async.run(() -> dao.budgetStatus(m), rows -> table.getItems().setAll(rows));
    }

    private void onSet() {
        Category c = category.getValue();
        String m = month.getValue();
        double lim;
        try {
            lim = Double.parseDouble(limit.getText().trim());
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, "Enter a numeric limit.", ButtonType.OK).show();
            return;
        }
        if (c == null || m == null || lim <= 0) {
            new Alert(Alert.AlertType.WARNING, "Pick a category and enter a limit greater than 0.", ButtonType.OK).show();
            return;
        }
        Async.run(() -> { dao.upsertBudget(c.id(), m, lim); return null; }, v -> {
            limit.clear();
            EventBus.publish(EventBus.Topic.BUDGETS_CHANGED);
        });
    }
}
