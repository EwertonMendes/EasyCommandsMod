package br.tblack.plugin;

import au.ellie.hyui.builders.HyUIHud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudStore {

    private static final Map<UUID, HyUIHud> huds = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> visibility = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> dirty = new ConcurrentHashMap<>();

    public static void setHud(UUID uuid, HyUIHud hud) {
        huds.put(uuid, hud);
    }

    public static HyUIHud getHud(UUID uuid) {
        return huds.get(uuid);
    }

    public static void removeHud(UUID uuid) {
        HyUIHud hud = huds.remove(uuid);
        if (hud != null) {
            hud.remove();
        }
    }

    public static void setIsVisible(UUID uuid, boolean isVisible) {
        visibility.put(uuid, isVisible);
    }

    public static boolean getIsVisible(UUID uuid) {
        return visibility.getOrDefault(uuid, true);
    }

    public static void markDirty(UUID uuid) {
        dirty.put(uuid, true);
    }

    public static boolean isDirty(UUID uuid) {
        return dirty.getOrDefault(uuid, false);
    }

    public static void clearDirty(UUID uuid) {
        dirty.put(uuid, false);
    }
}