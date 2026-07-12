package com.circuitsokoban.game;

/**
 * Screen navigation, implemented by the {@code Game}. Screens call these to move
 * around without knowing about each other.
 */
public interface Navigator {

    void playLevel(Tier tier, long seed);

    void showMenu();
}
