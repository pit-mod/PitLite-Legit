package com.pitlite.gui.physics;

public final class PhysicsSpring {

    public static final float IOS_BOUNCY_TENSION = 320f;
    public static final float IOS_BOUNCY_FRICTION = 11f;

    public static final float IOS_FLUID_TENSION = 175f;
    public static final float IOS_FLUID_FRICTION = 27f;

    public static final float IOS_SNAPPY_TENSION = 420f;
    public static final float IOS_SNAPPY_FRICTION = 32f;

    private static final float MAX_STEP_SECONDS = 1f / 120f;
    private static final float MAX_FRAME_SECONDS = 0.05f;
    private static final float SETTLE_EPSILON = 0.0005f;

    private float targetValue;
    private float currentValue;
    private float velocity;
    private float tension;
    private float friction;

    public PhysicsSpring(float initialValue, float tension, float friction) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.tension = tension;
        this.friction = friction;
    }

    public static PhysicsSpring iOSBouncy(float initialValue) {
        return new PhysicsSpring(initialValue, IOS_BOUNCY_TENSION, IOS_BOUNCY_FRICTION);
    }

    public static PhysicsSpring iOSFluid(float initialValue) {
        return new PhysicsSpring(initialValue, IOS_FLUID_TENSION, IOS_FLUID_FRICTION);
    }

    public static PhysicsSpring withPreset(Preset preset, float initialValue) {
        switch (preset) {
            case IOS_BOUNCY:
                return iOSBouncy(initialValue);
            case IOS_SNAPPY:
                return new PhysicsSpring(initialValue, IOS_SNAPPY_TENSION, IOS_SNAPPY_FRICTION);
            case IOS_FLUID:
            default:
                return iOSFluid(initialValue);
        }
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public void setTarget(float target) {
        this.targetValue = target;
    }

    public void setCurrent(float value) {
        this.currentValue = value;
    }

    public void snapTo(float value) {
        this.currentValue = value;
        this.targetValue = value;
        this.velocity = 0f;
    }

    public void setTension(float tension) {
        this.tension = tension;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public void applyPreset(Preset preset) {
        switch (preset) {
            case IOS_BOUNCY:
                tension = IOS_BOUNCY_TENSION;
                friction = IOS_BOUNCY_FRICTION;
                break;
            case IOS_FLUID:
                tension = IOS_FLUID_TENSION;
                friction = IOS_FLUID_FRICTION;
                break;
            case IOS_SNAPPY:
                tension = IOS_SNAPPY_TENSION;
                friction = IOS_SNAPPY_FRICTION;
                break;
            default:
                break;
        }
    }

    public float getTargetValue() {
        return targetValue;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public float getVelocity() {
        return velocity;
    }

    public boolean isSettled() {
        float displacement = currentValue - targetValue;
        return Math.abs(displacement) <= SETTLE_EPSILON && Math.abs(velocity) <= SETTLE_EPSILON;
    }

    public void update(float deltaTimeSeconds) {
        if (deltaTimeSeconds <= 0f) {
            return;
        }

        float dt = Math.min(deltaTimeSeconds, MAX_FRAME_SECONDS);
        float remaining = dt;

        while (remaining > 0f) {
            float step = Math.min(remaining, MAX_STEP_SECONDS);
            integrate(step);
            remaining -= step;
        }

        if (isSettled()) {
            currentValue = targetValue;
            velocity = 0f;
        }
    }

    private void integrate(float dt) {
        float displacement = currentValue - targetValue;
        float force = -tension * displacement - friction * velocity;

        velocity += force * dt;
        currentValue += velocity * dt;
    }

    public enum Preset {
        IOS_BOUNCY,
        IOS_FLUID,
        IOS_SNAPPY
    }

    public static final class DeltaTimer {

        private long lastNanos;
        private boolean primed;

        private DeltaTimer() {
        }

        public static DeltaTimer create() {
            return new DeltaTimer();
        }

        public float tickSeconds() {
            long now = System.nanoTime();
            if (!primed) {
                primed = true;
                lastNanos = now;
                return 1f / 60f;
            }
            float dt = (now - lastNanos) / 1_000_000_000f;
            lastNanos = now;
            return dt;
        }

        public void reset() {
            primed = false;
        }
    }
}
