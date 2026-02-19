package br.tblack.plugin.input;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionTracker {

    private InteractionTracker() {}

    public static final Map<UUID, Long> lastUsePress = new ConcurrentHashMap<>();
    public static final long USE_VALID_MS = 500;

    public static final Map<UUID, Boolean> pendingO = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> triggeredShortcut = new ConcurrentHashMap<>();

    public static final Map<UUID, Long> suppressShortcutUntil = new ConcurrentHashMap<>();
    public static final long SUPPRESS_AFTER_MANUAL_GM_MS = 250;

    public static final Map<UUID, Boolean> ctrlConsumed = new ConcurrentHashMap<>();

    public static final Map<UUID, Long> oCooldownUntil = new ConcurrentHashMap<>();

    public static boolean isShortcutSuppressed(UUID uuid) {
        Long until = suppressShortcutUntil.get(uuid);
        if (until == null) return false;

        long now = System.currentTimeMillis();
        if (now <= until) return true;

        suppressShortcutUntil.remove(uuid);
        return false;
    }

    public static boolean isOCooldown(UUID uuid) {
        Long until = oCooldownUntil.get(uuid);
        if (until == null) return false;

        long now = System.currentTimeMillis();
        if (now <= until) return true;

        oCooldownUntil.remove(uuid);
        return false;
    }

    public static void startOCooldown(UUID uuid) {
        oCooldownUntil.put(uuid, System.currentTimeMillis() + USE_VALID_MS);
    }
}
