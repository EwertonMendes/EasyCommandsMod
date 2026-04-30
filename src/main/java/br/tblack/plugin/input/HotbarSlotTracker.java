package br.tblack.plugin.input;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class HotbarSlotTracker {

    private static final ConcurrentMap<UUID, Integer> ACTIVE_SLOT_BY_PLAYER = new ConcurrentHashMap<>();

    private HotbarSlotTracker() {
    }

    public static void register() {
        PlayerPacketWatcher watcher = HotbarSlotTracker::handleInboundPacket;
        PacketAdapters.registerInbound(watcher);
    }

    public static int getOneBased(UUID uuid) {
        if (uuid == null) return 1;

        return ACTIVE_SLOT_BY_PLAYER.getOrDefault(uuid, 1);
    }

    public static void setZeroBased(UUID uuid, int zeroBasedSlot) {
        if (uuid == null) return;
        if (zeroBasedSlot < 0 || zeroBasedSlot > 8) return;

        ACTIVE_SLOT_BY_PLAYER.put(uuid, zeroBasedSlot + 1);
    }

    public static void clear(UUID uuid) {
        if (uuid == null) return;

        ACTIVE_SLOT_BY_PLAYER.remove(uuid);
    }

    private static void handleInboundPacket(PlayerRef playerRef, Packet packet) {
        if (playerRef == null || packet == null) return;
        if (!(packet instanceof SyncInteractionChains syncInteractionChains)) return;
        if (syncInteractionChains.updates == null) return;

        UUID uuid = playerRef.getUuid();

        for (SyncInteractionChain update : syncInteractionChains.updates) {
            if (update == null) continue;

            if (update.interactionType == InteractionType.SwapFrom
                    && update.initial
                    && update.data != null
                    && update.data.targetSlot >= 0) {
                setZeroBased(uuid, update.data.targetSlot);
                continue;
            }

            if (update.activeHotbarSlot >= 0) {
                setZeroBased(uuid, update.activeHotbarSlot);
            }
        }
    }
}