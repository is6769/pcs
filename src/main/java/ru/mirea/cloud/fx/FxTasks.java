package ru.mirea.cloud.fx;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** JDBC and file I/O run on one worker; UI callbacks always run on the JavaFX thread. */
public final class FxTasks implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread worker = new Thread(runnable, "s-cloud-data");
        worker.setDaemon(true);
        return worker;
    });
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper(false);
    private int pending;
    private boolean closed;

    public ReadOnlyBooleanProperty busyProperty() { return busy.getReadOnlyProperty(); }
    public boolean isBusy() { return busy.get(); }

    public <T> void submit(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Submit tasks from the JavaFX thread.");
        if (closed) throw new IllegalStateException("Application is closed.");
        pending++;
        busy.set(true);
        executor.submit(() -> {
            T value;
            try {
                value = work.call();
            } catch (Exception error) {
                Platform.runLater(() -> complete(() -> failure.accept(error)));
                return;
            }
            Platform.runLater(() -> complete(() -> success.accept(value)));
        });
    }

    private void complete(Runnable callback) {
        pending--;
        busy.set(pending > 0);
        if (!closed) callback.run();
    }

    @Override public void close() {
        closed = true;
        executor.shutdownNow();
    }
}
