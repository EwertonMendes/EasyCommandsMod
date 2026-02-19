package br.tblack.plugin.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Locale;
import java.util.function.Consumer;

public class EasyCommandsUtils {

    public static String normalizeCommand(String rawCommandInput) {
        if (rawCommandInput == null) return "";

        String normalizedCommand = rawCommandInput.trim();
        if (normalizedCommand.isEmpty()) return "";

        if (normalizedCommand.startsWith("/")) normalizedCommand = normalizedCommand.substring(1).trim();
        if (normalizedCommand.isEmpty()) return "";

        return normalizedCommand.toLowerCase(Locale.ROOT);
    }

    public static void runOnWorldThread(PlayerRef playerApiRef, Consumer<Player> action) {
        if (playerApiRef == null || action == null) return;

        var playerEntityReference = playerApiRef.getReference();
        if (playerEntityReference == null || !playerEntityReference.isValid()) return;

        var entityStore = playerEntityReference.getStore();
        var world = entityStore.getExternalData().getWorld();

        world.execute(() -> {
            Player playerEntity = entityStore.getComponent(playerEntityReference, Player.getComponentType());
            if (playerEntity == null) return;

            action.accept(playerEntity);
        });
    }
}
