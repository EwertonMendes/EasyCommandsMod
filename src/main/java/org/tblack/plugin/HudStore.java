package org.tblack.plugin;

import au.ellie.hyui.builders.HyUIHud;

public class HudStore {

    private static HyUIHud hud;
    private static boolean isVisible;

    public static void setHud(HyUIHud newHud) {
        hud = newHud;
    }

    public static HyUIHud getHud() {
        return hud;
    }

    public static void setIsVisible(boolean isHidden) {
        isVisible = isHidden;
    }

    public static boolean getIsVisible() {
        return isVisible;
    }

}
