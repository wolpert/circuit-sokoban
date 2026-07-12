package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;

/**
 * One abstract particle (a small square or triangle) for burst effects. Kept
 * deliberately minimal &mdash; the brief asks for simple geometric bursts, not a
 * full particle system.
 */
final class Particle {

    float x;
    float y;
    float vx;
    float vy;
    float life;        // seconds remaining
    final float maxLife;
    float size;
    float angle;       // degrees
    float spin;        // degrees / second
    final Color color;
    final boolean triangle;

    Particle(float x, float y, float vx, float vy, float life, float size,
             float spin, Color color, boolean triangle) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.size = size;
        this.spin = spin;
        this.color = color;
        this.triangle = triangle;
    }

    boolean update(float dt) {
        x += vx * dt;
        y += vy * dt;
        vy -= 220f * dt;   // gentle gravity so bursts arc and settle
        vx *= (1f - 1.4f * dt);
        angle += spin * dt;
        life -= dt;
        return life > 0f;
    }

    /** 0..1, fades out over the particle's life. */
    float alpha() {
        return Math.max(0f, life / maxLife);
    }
}
