package com.pitlite.module;

public interface DraggableHud {
    String getHudKey();

    boolean isHudVisible();

    int getHudX();

    int getHudY();

    int getHudWidth();

    int getHudHeight();

    default boolean isHudCenterAnchored() {
        return false;
    }

    default int getRenderX() {
        return com.pitlite.utils.HudBounds.clampX(
                getHudX(), Math.max(1, getHudWidth()), isHudCenterAnchored());
    }

    default int getRenderY() {
        return com.pitlite.utils.HudBounds.clampY(
                com.pitlite.utils.HudStackManager.getStackedY(this), Math.max(1, getHudHeight()));
    }
}
