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
    private static final Map<UUID, Integer> highlightedSlot = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> nextAttachAtMs = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHudUpdateMs = new ConcurrentHashMap<>();

    public static void setHud(UUID uuid, HyUIHud hud) {
        huds.put(uuid, hud);
    }

    public static HyUIHud getHud(UUID uuid) {
        return huds.get(uuid);
    }

    public static void removeHud(UUID uuid) {
        HyUIHud hud = huds.remove(uuid);
        if (hud == null) return;

        try {
            hud.remove();
        } catch (Exception ignored) {
        }
    }

    public static void clearPlayer(UUID uuid) {
        removeHud(uuid);
        visibility.remove(uuid);
        dirty.remove(uuid);
        position.remove(uuid);
        highlightedSlot.remove(uuid);
        nextAttachAtMs.remove(uuid);
        lastHudUpdateMs.remove(uuid);
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

    public static void setHighlightedSlot(UUID uuid, int slotOrMinusOne) {
        highlightedSlot.put(uuid, slotOrMinusOne);
    }

    public static int getHighlightedSlot(UUID uuid) {
        return highlightedSlot.getOrDefault(uuid, -1);
    }

    public static void setNextAttachAtMs(UUID uuid, long atMs) {
        nextAttachAtMs.put(uuid, atMs);
    }

    public static long getNextAttachAtMs(UUID uuid) {
        return nextAttachAtMs.getOrDefault(uuid, 0L);
    }

    public static boolean canAttachNow(UUID uuid, long nowMs) {
        return nowMs >= getNextAttachAtMs(uuid);
    }

    public static long getLastHudUpdateMs(UUID uuid) {
        return lastHudUpdateMs.getOrDefault(uuid, 0L);
    }

    public static void setLastHudUpdateMs(UUID uuid, long nowMs) {
        lastHudUpdateMs.put(uuid, nowMs);
    }

    public static boolean canUpdateNow(UUID uuid, long nowMs, long minIntervalMs) {
        long last = getLastHudUpdateMs(uuid);
        return nowMs - last >= minIntervalMs;
    }
}