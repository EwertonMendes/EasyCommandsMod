package br.tblack.plugin.hud;

import au.ellie.hyui.builders.HyUIHud;
import br.tblack.plugin.enums.HudPositionPreset;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudStore {

    private static final Map<UUID, HyUIHud> huds = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> visibility = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> dirty = new ConcurrentHashMap<>();
    private static final Map<UUID, HudPositionPreset> position = new ConcurrentHashMap<>();

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

    public static void setPosition(UUID uuid, HudPositionPreset preset) {
        position.put(uuid, preset == null ? HudPositionPreset.TOP_LEFT : preset);
    }

    public static HudPositionPreset getPosition(UUID uuid) {
        return position.getOrDefault(uuid, HudPositionPreset.TOP_LEFT);
    }
}
