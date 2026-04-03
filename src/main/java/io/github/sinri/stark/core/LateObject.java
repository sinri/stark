package io.github.sinri.stark.core;

import io.vertx.core.Future;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A lazy-initialization container that holds a single value of type {@code T}.
 * The value can be set once via {@link #initialize(T)} or lazily resolved
 * via {@link #ensureSync(Supplier)} / {@link #ensureAsync(Supplier)}.
 *
 * <p><b>Thread safety:</b> This class is <em>not</em> thread-safe.
 * It must be confined to a single Vert.x event-loop thread or
 * externally synchronized if shared across threads.</p>
 */
public final class LateObject<T> {
    private @Nullable T embedded;

    /**
     * Return the held value if it has been initialized,
     * or throw {@link NullPointerException}.
     */
    public T get() {
        return Objects.requireNonNull(embedded, "LateObject has not been initialized");
    }

    /**
     * Return the held value, or {@code null} if not yet initialized.
     */
    public @Nullable T getOrNull() {
        return embedded;
    }

    /**
     * Return {@code true} if the value has been initialized.
     */
    public boolean isInitialized() {
        return embedded != null;
    }

    /**
     * Set the value if it has not been initialized yet.
     *
     * @throws IllegalStateException if the value has already been initialized
     */
    public void initialize(T value) {
        if (embedded != null) {
            throw new IllegalStateException("LateObject has already been initialized");
        }
        this.embedded = value;
    }

    /**
     * Return the held value if initialized; otherwise compute it from
     * the given supplier, store and return it.
     *
     * @param supplier must not return {@code null}
     * @throws NullPointerException if the supplier returns {@code null}
     */
    public T ensureSync(Supplier<T> supplier) {
        if (embedded == null) {
            embedded = Objects.requireNonNull(supplier.get(), "supplier must not return null");
        }
        return embedded;
    }

    /**
     * Return a succeeded {@link Future} with the held value if initialized;
     * otherwise compute it asynchronously from the given supplier,
     * store and return it.
     *
     * @param supplier must not produce a {@code null} value
     */
    public Future<T> ensureAsync(Supplier<Future<T>> supplier) {
        if (embedded != null) {
            return Future.succeededFuture(embedded);
        }
        return supplier.get()
                .compose(t -> {
                    Objects.requireNonNull(t, "async supplier must not produce null");
                    this.embedded = t;
                    return Future.succeededFuture(t);
                });
    }
}
