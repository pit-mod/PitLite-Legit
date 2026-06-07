package com.pitlite.gui.physics;

import java.util.HashMap;
import java.util.Map;

public final class GuiAnimationController {

    private static GuiAnimationController active;

    private final PhysicsSpring.DeltaTimer deltaTimer = PhysicsSpring.DeltaTimer.create();
    private final Map<String, PhysicsSpring> springs = new HashMap<>();

    private boolean reduceAnimations;
    private long openedAtMs;
    private float frameDt = 1f / 60f;
    private float scrollY;
    private float scrollTargetY;
    private float scrollVelocity;

    public static GuiAnimationController get() {
        return active;
    }

    public static void bind(GuiAnimationController controller) {
        active = controller;
    }

    public static void unbind() {
        active = null;
    }

    public void onGuiOpened() {
        openedAtMs = System.currentTimeMillis();
        deltaTimer.reset();
        scrollY = 0f;
        scrollTargetY = 0f;
        scrollVelocity = 0f;
        springs.clear();
    }

    public long getOpenedAtMs() {
        return openedAtMs;
    }

    public float getGuiOpenT() {
        return getValue("gui:open");
    }

    public float animateStagger(String id, float target, int index, int delayMs, PhysicsSpring.Preset preset) {
        long elapsed = System.currentTimeMillis() - openedAtMs;
        if (elapsed < (long) index * delayMs) {
            target = 0f;
        }
        return animateClamped01(id, target, preset);
    }

    public void beginFrame() {
        frameDt = reduceAnimations ? (1f / 60f) : deltaTimer.tickSeconds();
    }

    public void setReduceAnimations(boolean reduceAnimations) {
        this.reduceAnimations = reduceAnimations;
    }

    public boolean isReduceAnimations() {
        return reduceAnimations;
    }

    public void primeSpring(String id, float initial, PhysicsSpring.Preset preset) {
        PhysicsSpring spring = PhysicsSpring.withPreset(preset, initial);
        springs.put(id, spring);
    }

    private PhysicsSpring springOf(String id, PhysicsSpring.Preset preset, float initialIfNew) {
        PhysicsSpring spring = springs.get(id);
        if (spring == null) {
            spring = PhysicsSpring.withPreset(preset, initialIfNew);
            springs.put(id, spring);
        }
        return spring;
    }

    public float animate(String id, float target, PhysicsSpring.Preset preset) {
        PhysicsSpring spring = springOf(id, preset, target);
        if (reduceAnimations) {
            spring.snapTo(target);
            return target;
        }
        spring.setTarget(target);
        spring.update(frameDt);
        return spring.getCurrentValue();
    }

    public float animateClamped01(String id, float target, PhysicsSpring.Preset preset) {
        PhysicsSpring spring = springOf(id, preset, target > 0.5f ? 1f : 0f);
        spring.applyPreset(preset);
        if (reduceAnimations) {
            spring.snapTo(target);
            return target;
        }
        spring.setTarget(target);
        spring.update(frameDt);
        if (target <= 0.001f && spring.getCurrentValue() < 0f) {
            spring.snapTo(0f);
        } else if (target >= 0.999f && spring.getCurrentValue() > 1f) {
            spring.snapTo(1f);
        }
        return GuiMath.clamp01(spring.getCurrentValue());
    }

    public float animate01(String id, boolean on, PhysicsSpring.Preset preset) {
        return animateClamped01(id, on ? 1f : 0f, preset);
    }

    public float animate01Stable(String id, boolean on) {
        float target = on ? 1f : 0f;
        PhysicsSpring spring = springs.get(id);
        if (spring == null) {
            spring = PhysicsSpring.withPreset(PhysicsSpring.Preset.IOS_FLUID, target);
            springs.put(id, spring);
            return target;
        }
        if (reduceAnimations) {
            spring.snapTo(target);
            return target;
        }
        spring.setTarget(target);
        spring.update(frameDt);
        return GuiMath.clamp01(spring.getCurrentValue());
    }

    public void removeSpring(String id) {
        springs.remove(id);
    }

    public void removeSpringsWithPrefix(String prefix) {
        springs.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void retargetClamped01(String id, float target) {
        retargetClamped01(id, target, PhysicsSpring.Preset.IOS_FLUID);
    }

    public void retargetClamped01(String id, float target, PhysicsSpring.Preset preset) {
        target = GuiMath.clamp01(target);
        PhysicsSpring spring = springs.get(id);
        if (spring == null) {
            float initial = target > 0.5f ? 0f : 1f;
            spring = PhysicsSpring.withPreset(preset, initial);
            spring.setTarget(target);
            springs.put(id, spring);
            return;
        }
        spring.applyPreset(preset);
        spring.setTarget(target);
        spring.setVelocity(0f);
    }

    public boolean isSettled(String id, float target) {
        PhysicsSpring spring = springs.get(id);
        if (spring == null) {
            return true;
        }
        spring.setTarget(target);
        return spring.isSettled();
    }

    public float getValue(String id) {
        PhysicsSpring spring = springs.get(id);
        return spring == null ? 0f : GuiMath.clamp01(spring.getCurrentValue());
    }

    public void applyScrollImpulse(int wheelDelta) {
        if (wheelDelta == 0) {
            return;
        }
        scrollVelocity -= wheelDelta * 0.4f;
    }

    public float tickScroll(int maxScroll) {
        float max = (float) maxScroll;

        if (reduceAnimations) {
            scrollTargetY = clamp(scrollTargetY + scrollVelocity, 0f, max);
            scrollVelocity = 0f;
            scrollY = scrollTargetY;
            return scrollY;
        }

        scrollTargetY += scrollVelocity;
        scrollTargetY = clamp(scrollTargetY, 0f, max);
        scrollVelocity *= 0.82f;
        if (Math.abs(scrollVelocity) < 0.15f) {
            scrollVelocity = 0f;
        }

        scrollY += (scrollTargetY - scrollY) * 0.38f;
        if (Math.abs(scrollTargetY - scrollY) < 0.35f) {
            scrollY = scrollTargetY;
        }
        return scrollY;
    }

    public void setScrollTarget(float target) {
        scrollTargetY = target;
    }

    public float getScrollY() {
        return scrollY;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
