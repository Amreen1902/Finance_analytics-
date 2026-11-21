package com.finance;

import com.finance.dao.FinanceDao;
import com.finance.db.Db;
import com.finance.db.Seed;
import com.finance.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Db.init();
        Seed.ifEmpty();
        FinanceDao dao = new FinanceDao();

        TabPane tabs = new TabPane(
                tab("Transactions", new TransactionsTab(dao)),
                tab("Dashboard", new DashboardTab(dao)),
                tab("Budgets", new BudgetsTab(dao)));

        BorderPane root = new BorderPane(tabs);

        // Drag-and-drop CSV import: works no matter which tab is open.
        root.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        root.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            boolean ok = e.getDragboard().hasFiles() && !files.isEmpty();
            if (ok) ImportAction.run(files);
            e.setDropCompleted(ok);
            e.consume();
        });

        Scene scene = new Scene(root, 1150, 760);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("Personal Finance Analytics");
        stage.setScene(scene);
        stage.show();
    }

    private static Tab tab(String title, Node content) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
