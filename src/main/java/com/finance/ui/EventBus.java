package com.finance.ui;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny publish/subscribe bus (observer pattern). Tabs never reference each other:
 * a write publishes a topic and every interested view reloads itself.
 * Listeners always run on the JavaFX thread.
 */
public final class EventBus {
    public enum Topic { TRANSACTIONS_CHANGED, BUDGETS_CHANGED }

    private static final Map<Topic, List<Runnable>> LISTENERS = new EnumMap<>(Topic.class);

    private EventBus() {}

    public static void subscribe(Topic topic, Runnable listener) {
        LISTENERS.computeIfAbsent(topic, t -> new ArrayList<>()).add(listener);
    }

    public static void publish(Topic topic) {
        Platform.runLater(() -> new ArrayList<>(LISTENERS.getOrDefault(topic, List.of())).forEach(Runnable::run));
    }
}
