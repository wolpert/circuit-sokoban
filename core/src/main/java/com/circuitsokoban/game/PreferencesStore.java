package com.circuitsokoban.game;

import com.badlogic.gdx.Preferences;

/** libGDX-{@link Preferences}-backed {@link Store}; persists to disk on desktop and Android. */
public final class PreferencesStore implements Store {

    private final Preferences prefs;

    public PreferencesStore(Preferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return prefs.getInteger(key, defaultValue);
    }

    @Override
    public void putInt(String key, int value) {
        prefs.putInteger(key, value);
    }

    @Override
    public void flush() {
        prefs.flush();
    }
}
