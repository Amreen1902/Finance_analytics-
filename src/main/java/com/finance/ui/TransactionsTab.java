package com.finance.ui;

import com.finance.dao.FinanceDao;
import com.finance.model.Models.Category;
import com.finance.model.Models.Txn;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** Ledger: live filters (FilteredList + SortedList), add/delete, CSV import. */
public class TransactionsTab extends BorderPane {
    private final FinanceDao dao;

    private final ObservableList<Txn> master = FXCollections.observableArrayList();
    private final FilteredList<Txn> filtered = new FilteredList<>(master, t -> true);
    private final TableView<Txn> table = new TableView<>();

    // filters
    private final TextField search = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>(FXCollections.observableArrayList("All", "Income", "Expense"));
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final Label summary = new Label();

    // add form
    private final DatePicker newDate = new DatePicker(LocalDate.now());
    private final TextField newDesc = new TextField();
    private final TextField newAmount = new TextField();
    private final ComboBox<Category> newCategory = new ComboBox<>();

    public TransactionsTab(FinanceDao dao) {
        this.dao = dao;
        setPadding(new Insets(12));
        setTop(new VBox(10, buildFilterBar(), buildAddBar()));
        setCenter(buildTable());
        setBottom(buildFooter());

        EventBus.subscribe(EventBus.Topic.TRANSACTIONS_CHANGED, this::reload);
        reload();
    }

    // ---------- UI construction ----------

    private HBox buildFilterBar() {
        search.setPromptText("Search description…");
        search.setPrefWidth(220);
        categoryFilter.setPrefWidth(150);
        typeFilter.setValue("All");
        from.setPromptText("From");
        to.setPromptText("To");
        from.setPrefWidth(130);
        to.setPrefWidth(130);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            search.clear(); categoryFilter.setValue("All"); typeFilter.setValue("All");
            from.setValue(null); to.setValue(null);
        });

        // Live filtering: any control change re-evaluates the predicate immediately.
        search.textProperty().addListener((o, a, b) -> applyFilter());
        categoryFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        typeFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        from.valueProperty().addListener((o, a, b) -> applyFilter());
        to.valueProperty().addListener((o, a, b) -> applyFilter());

        HBox bar = new HBox(8, search, categoryFilter, typeFilter, from, to, clear);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox buildAddBar() {
        newDesc.setPromptText("Description");
        newDesc.setPrefWidth(220);
        newAmount.setPromptText("Amount (-450 expense, 5000 income)");
        newAmount.setPrefWidth(240);
        newCategory.setPromptText("Category");
        newCategory.setPrefWidth(150);
        newDate.setPrefWidth(130);

        Button add = new Button("Add");
        add.setDefaultButton(false);
        add.setOnAction(e -> onAdd());
        Button delete = new Button("Delete selected");
        delete.setOnAction(e -> onDelete());
        Button importBtn = new Button("Import CSV…");
        importBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            List<java.io.File> files = fc.showOpenMultipleDialog(getScene().getWindow());
            if (files != null) ImportAction.run(files);
        });

        Button exportBtn = new Button("Export CSV…");
        exportBtn.setOnAction(e -> onExport());

        HBox bar = new HBox(8, newDate, newDesc, newAmount, newCategory, add, new Separator(), delete, importBtn, exportBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private TableView<Txn> buildTable() {
        TableColumn<Txn, LocalDate> date = new TableColumn<>("Date");
        date.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().date()));
        date.setPrefWidth(110);

        TableColumn<Txn, String> desc = new TableColumn<>("Description");
        desc.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().description()));
        desc.setPrefWidth(300);

        TableColumn<Txn, String> cat = new TableColumn<>("Category");
        cat.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().category()));
        cat.setPrefWidth(140);

        TableColumn<Txn, Double> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().amount()));
        amount.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(Fmt.money(v));
                setStyle("-fx-text-fill:" + (v < 0 ? "#c0392b" : "#1e8449") + "; -fx-alignment: CENTER-RIGHT;");
            }
        });
        amount.setPrefWidth(130);

        TableColumn<Txn, Double> balance = new TableColumn<>("Running balance");
        balance.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().balance()));
        balance.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : Fmt.money(v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
        balance.setPrefWidth(150);

        table.getColumns().addAll(date, desc, cat, amount, balance);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No transactions match. Drop a .csv file anywhere to import."));

        SortedList<Txn> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        BorderPane.setMargin(table, new Insets(10, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private HBox buildFooter() {
        summary.setStyle("-fx-font-weight: bold;");
        HBox box = new HBox(summary);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    // ---------- behaviour ----------

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().toLowerCase().trim();
        String cat = categoryFilter.getValue();
        String type = typeFilter.getValue();
        LocalDate f = from.getValue(), t = to.getValue();

        filtered.setPredicate(x ->
                (q.isEmpty() || x.description().toLowerCase().contains(q))
                && (cat == null || "All".equals(cat) || x.category().equals(cat))
                && (type == null || "All".equals(type) || ("Income".equals(type) == x.amount() > 0))
                && (f == null || !x.date().isBefore(f))
                && (t == null || !x.date().isAfter(t)));
        updateSummary();
    }

    private void updateSummary() {
        double in = 0, out = 0;
        for (Txn t : filtered) { if (t.amount() > 0) in += t.amount(); else out += -t.amount(); }
        summary.setText(String.format("%d rows   |   Income %s   |   Expense %s   |   Net %s",
                filtered.size(), Fmt.money(in), Fmt.money(out), Fmt.money(in - out)));
    }

    private void reload() {
        Async.run(() -> new Loaded(dao.transactionsWithBalance(), dao.categories()), data -> {
            master.setAll(data.txns());

            String keep = categoryFilter.getValue();
            ObservableList<String> names = FXCollections.observableArrayList("All");
            data.cats().forEach(c -> names.add(c.name()));
            categoryFilter.setItems(names);
            categoryFilter.setValue(keep != null && names.contains(keep) ? keep : "All");

            Category keepNew = newCategory.getValue();
            newCategory.setItems(FXCollections.observableArrayList(data.cats()));
            if (keepNew != null) data.cats().stream().filter(c -> c.id() == keepNew.id()).findFirst().ifPresent(newCategory::setValue);

            applyFilter();
        });
    }

    private record Loaded(List<Txn> txns, List<Category> cats) {}

    private void onAdd() {
        LocalDate d = newDate.getValue();
        String desc = newDesc.getText() == null ? "" : newDesc.getText().trim();
        Category cat = newCategory.getValue();
        double amount;
        try {
            amount = Double.parseDouble(newAmount.getText().trim());
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.WARNING, "Enter a numeric amount.", ButtonType.OK).show();
            return;
        }
        if (d == null || desc.isEmpty() || cat == null || amount == 0) {
            new Alert(Alert.AlertType.WARNING, "Date, description, category and a non-zero amount are required.", ButtonType.OK).show();
            return;
        }
        Async.run(() -> { dao.add(d, desc, amount, cat.id()); return null; }, v -> {
            newDesc.clear();
            newAmount.clear();
            EventBus.publish(EventBus.Topic.TRANSACTIONS_CHANGED);
        });
    }

    private void onDelete() {
        Txn sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.description() + "\" (" + Fmt.money(sel.amount()) + ")?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b ->
                Async.run(() -> { dao.delete(sel.id()); return null; },
                        v -> EventBus.publish(EventBus.Topic.TRANSACTIONS_CHANGED)));
    }

    private void onExport() {
        if (filtered.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No transactions to export.", ButtonType.OK).show();
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Transactions to CSV");
        fc.setInitialFileName("transactions-export.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        java.io.File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        List<Txn> toExport = List.copyOf(filtered);
        Async.run(() -> {
            com.finance.dao.CsvExporter.exportToFile(file, toExport);
            return toExport.size();
        }, count -> new Alert(Alert.AlertType.INFORMATION,
                "Successfully exported " + count + " transaction(s) to " + file.getName(),
                ButtonType.OK).show());
    }
}
