package br.tblack.plugin;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class PacketListener implements PlayerPacketFilter {

    private static final int PACKET_SYNC_INTERACTION_CHAINS = 290;
    private static final int PACKET_SET_GAMEMODE = 101;
    private static final int PACKET_CHAT_MESSAGE = 211;

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
            case PACKET_CHAT_MESSAGE -> onChat(player, playerUuid, activationMode, packet);
            case PACKET_SET_GAMEMODE -> onSetGameMode(playerUuid, activationMode);
            case PACKET_SYNC_INTERACTION_CHAINS -> onInteractionChains(playerUuid, activationMode, packet);
            default -> false;
        };
    }

    private boolean onChat(PlayerRef playerRefApi, UUID uuid, PlayerConfig.ActivationMode mode, Packet packet) {
        if (mode != PlayerConfig.ActivationMode.O_ONLY) return false;

        String msg = tryReadChatMessage(packet);
        if (msg == null) return false;

        String normalized = normalize(msg);

        if (PERMITTED_GAMEMODE_COMMANDS.contains(normalized)) {
            long now = System.currentTimeMillis();

            InteractionTracker.suppressShortcutUntil.put(uuid, now + InteractionTracker.SUPPRESS_AFTER_MANUAL_GM_MS);

            InteractionTracker.pendingO.remove(uuid);
            InteractionTracker.triggeredShortcut.remove(uuid);
            InteractionTracker.blockNextSetGameMode.remove(uuid);
            InteractionTracker.lastAutoGmChatAt.remove(uuid);

            runOnWorldThread(playerRefApi, player -> {
                CommandManager.get().handleCommand(player, normalized);
            });

            return true;
        }

        if (!AUTO_GAMEMODE_COMMANDS.contains(normalized)) return false;

        InteractionTracker.pendingO.put(uuid, true);
        InteractionTracker.triggeredShortcut.remove(uuid);

        InteractionTracker.blockNextSetGameMode.put(uuid, true);
        InteractionTracker.lastAutoGmChatAt.put(uuid, System.currentTimeMillis());

        return true;
    }

    private boolean onSetGameMode(UUID uuid, PlayerConfig.ActivationMode mode) {
        if (InteractionTracker.isShortcutSuppressed(uuid)) {
            return false;
        }

        if (mode != PlayerConfig.ActivationMode.O_ONLY) return false;

        if (Boolean.TRUE.equals(InteractionTracker.blockNextSetGameMode.remove(uuid))) {
            return true;
        }

        Long t = InteractionTracker.lastAutoGmChatAt.get(uuid);
        if (t == null) return false;

        long now = System.currentTimeMillis();
        if (now - t <= InteractionTracker.AUTO_GM_BLOCK_MS) {
            return true;
        }

        InteractionTracker.lastAutoGmChatAt.remove(uuid);
        return false;
    }

    private boolean onInteractionChains(UUID uuid, PlayerConfig.ActivationMode mode, Packet packet) {
        if (InteractionTracker.isShortcutSuppressed(uuid)) {
            return false;
        }

        SyncInteractionChain[] updates = ((SyncInteractionChains) packet).updates;
        if (updates == null || updates.length == 0) return false;

        for (SyncInteractionChain item : updates) {
            InteractionType type = item.interactionType;

            if (mode == PlayerConfig.ActivationMode.CTRL_F) {
                if (type == InteractionType.Use) {
                    InteractionTracker.lastUsePress.put(uuid, System.currentTimeMillis());
                }
                continue;
            }

            if (mode == PlayerConfig.ActivationMode.O_ONLY && isGameModeSwapTrigger(type)) {
                InteractionTracker.pendingO.put(uuid, true);
                InteractionTracker.triggeredShortcut.remove(uuid);
                InteractionTracker.blockNextSetGameMode.put(uuid, true);
                return true;
            }
        }

        return false;
    }

    private static boolean isGameModeSwapTrigger(InteractionType type) {
        return type == InteractionType.GameModeSwap;
    }

    private static String normalize(String s) {
        String t = s.trim();
        if (t.startsWith("/")) t = t.substring(1).trim();
        return t.toLowerCase(Locale.ROOT);
    }

    private static String tryReadChatMessage(Packet packet) {
        Object value = tryReadField(packet, "message");
        return (value instanceof String str) ? str : null;
    }

    private static Object tryReadField(Object target, String fieldName) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static void runOnWorldThread(PlayerRef playerApiRef, Consumer<Player> action) {
        var ref = playerApiRef.getReference();
        if (ref == null || !ref.isValid()) return;

        var store = ref.getStore();
        var world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                action.accept(player);
            }
        });
    }
}
