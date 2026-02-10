package org.tblack.plugin;

import au.ellie.hyui.builders.HyUIHud;

public class HudStore {

    private static HyUIHud hud;

    public static void setHud(HyUIHud newHud) {
        hud = newHud;
    }

    public static HyUIHud getHud() {
        return hud;
    }

}
