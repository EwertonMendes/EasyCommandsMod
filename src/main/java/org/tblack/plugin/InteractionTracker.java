package org.tblack.plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionTracker {
    public static final Map<UUID, Long> lastUsePress = new ConcurrentHashMap<>();
    public static final long USE_VALID_MS = 300;

    public static final Map<UUID, Boolean> triggeredShortcut = new ConcurrentHashMap<>();
}
