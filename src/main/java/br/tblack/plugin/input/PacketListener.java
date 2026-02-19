package br.tblack.plugin.input;

import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.util.EasyCommandsUtils;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public final class PacketListener implements PlayerPacketFilter {

    private static final Set<String> AUTO_GAMEMODE_COMMANDS = Set.of(
            "gm c", "gm a"
    );

    private static final Set<String> PERMITTED_GAMEMODE_COMMANDS = Set.of(
            "gamemode adventure", "gamemode creative",
            "gamemode a", "gamemode c"
    );

    @Override
    public boolean test(@Nonnull PlayerRef player, @Nonnull Packet packet) {
        UUID playerUuid = player.getUuid();
        String playerId = playerUuid.toString();
        PlayerConfig.ActivationMode activationMode = PlayerConfig.getActivationMode(playerId);

        return switch (packet.getId()) {
            case ChatMessage.PACKET_ID -> onChat(player, playerUuid, activationMode, packet);
            case SyncInteractionChains.PACKET_ID -> {
                onInteractionChains(playerUuid, activationMode, packet);
                yield false;
            }
            default -> false;
        };
    }

    private boolean onChat(PlayerRef playerRefApi, UUID uuid, PlayerConfig.ActivationMode mode, Packet packet) {
        if (mode != PlayerConfig.ActivationMode.O_ONLY) return false;
        if (!(packet instanceof ChatMessage chat)) return false;

        String msg = chat.message;
        if (msg == null) return false;

        String normalized = EasyCommandsUtils.normalizeCommand(msg);

        if (PERMITTED_GAMEMODE_COMMANDS.contains(normalized)) {
            long now = System.currentTimeMillis();

            InteractionTracker.suppressShortcutUntil.put(uuid, now + InteractionTracker.SUPPRESS_AFTER_MANUAL_GM_MS);

            InteractionTracker.pendingO.remove(uuid);
            InteractionTracker.triggeredShortcut.remove(uuid);

            EasyCommandsUtils.runOnWorldThread(playerRefApi, player -> {
                CommandManager.get().handleCommand(player, normalized);
            });

            return true;
        }

        if (!AUTO_GAMEMODE_COMMANDS.contains(normalized)) return false;

        InteractionTracker.pendingO.put(uuid, true);
        InteractionTracker.triggeredShortcut.remove(uuid);

        return true;
    }

    private void onInteractionChains(UUID uuid, PlayerConfig.ActivationMode mode, Packet packet) {
        if (mode != PlayerConfig.ActivationMode.CTRL_F) return;
        if (InteractionTracker.isShortcutSuppressed(uuid)) return;

        SyncInteractionChain[] updates = ((SyncInteractionChains) packet).updates;
        if (updates.length == 0) return;

        long now = System.currentTimeMillis();

        for (SyncInteractionChain item : updates) {
            if (item == null) continue;
            if (item.interactionType == InteractionType.Use) {
                InteractionTracker.lastUsePress.put(uuid, now);
                break;
            }
        }
    }

}
