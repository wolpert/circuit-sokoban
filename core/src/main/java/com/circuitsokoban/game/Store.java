package com.circuitsokoban.game;

/**
 * Minimal key/value persistence used by {@link Progress}. Abstracted so the
 * progress logic can be unit-tested with an in-memory store, and backed by
 * libGDX {@code Preferences} at runtime (works on desktop and Android alike).
 */
public interface Store {

    int getInt(String key, int defaultValue);

    void putInt(String key, int value);

    void flush();
}
