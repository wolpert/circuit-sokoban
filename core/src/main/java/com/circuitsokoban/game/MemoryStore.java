package com.circuitsokoban.game;

import java.util.HashMap;
import java.util.Map;

/** In-memory {@link Store}: for tests and for headless/screenshot runs (no persistence). */
public final class MemoryStore implements Store {

    private final Map<String, Integer> map = new HashMap<>();

    @Override
    public int getInt(String key, int defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    @Override
    public void putInt(String key, int value) {
        map.put(key, value);
    }

    @Override
    public void flush() {
        // nothing to persist
    }
}
