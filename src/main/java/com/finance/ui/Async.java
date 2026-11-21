package com.finance.ui;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs DB work off the UI thread. A SINGLE worker thread keeps SQLite writes
 * serialised (no "database is locked") and preserves ordering of operations.
 */
public final class Async {
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "db-worker");
        t.setDaemon(true);
        return t;
    });

    private Async() {}

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> error(task.getException()));
        POOL.execute(task);
    }

    public static void error(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.toString();
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
