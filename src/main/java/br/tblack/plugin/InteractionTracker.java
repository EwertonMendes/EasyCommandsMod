package br.tblack.plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionTracker {

    private InteractionTracker() {}

    public static final Map<UUID, Long> lastUsePress = new ConcurrentHashMap<>();
    public static final long USE_VALID_MS = 300;

    public static final Map<UUID, Boolean> pendingO = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> triggeredShortcut = new ConcurrentHashMap<>();

    public static final Map<UUID, Boolean> blockNextSetGameMode = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> lastAutoGmChatAt = new ConcurrentHashMap<>();
    public static final long AUTO_GM_BLOCK_MS = 600;

    public static final Map<UUID, Long> suppressShortcutUntil = new ConcurrentHashMap<>();
    public static final long SUPPRESS_AFTER_MANUAL_GM_MS = 250;

    public static boolean isShortcutSuppressed(UUID uuid) {
        Long until = suppressShortcutUntil.get(uuid);
        if (until == null) return false;

        long now = System.currentTimeMillis();
        if (now <= until) return true;

        suppressShortcutUntil.remove(uuid);
        return false;
    }
}
