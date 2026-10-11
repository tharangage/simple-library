package com.ascendion.roshan.simple_library.security;

import java.util.Optional;

/**
 * Keeps the {@link CurrentUser} of the request that the current thread is serving. Set by
 * {@link CurrentUserInterceptor} before the controller runs and always cleared afterwards, because
 * servlet threads are reused for other requests.
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    static void set(final CurrentUser user) {
        CURRENT.set(user);
    }

    static void clear() {
        CURRENT.remove();
    }

    /** The caller, or empty when the request carries no token (for example self-registration). */
    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** The caller of a protected endpoint. Throws if there is none, which would be a configuration error. */
    public static CurrentUser require() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user on this request"));
    }
}
