package com.circuitsokoban.game;

import com.circuitsokoban.solver.GenParams;

/**
 * A difficulty tier. Each tier is an <em>endless</em> stream of generated levels:
 * level N of a tier is {@code generate(seed = N, tier.params())}, so the seed is
 * simply the level index &mdash; reproducible and shareable.
 */
public enum Tier {
    EASY("Easy", GenParams.easy()),
    MEDIUM("Medium", GenParams.medium()),
    HARD("Hard", GenParams.hard());

    private final String displayName;
    private final GenParams params;

    Tier(String displayName, GenParams params) {
        this.displayName = displayName;
        this.params = params;
    }

    public String displayName() {
        return displayName;
    }

    public GenParams params() {
        return params;
    }
}
