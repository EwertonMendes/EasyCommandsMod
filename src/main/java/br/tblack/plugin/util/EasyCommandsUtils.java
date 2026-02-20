package br.tblack.plugin.util;

import au.ellie.hyui.events.UIContext;
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

    public static String readStringValue(UIContext uiContext, String elementId) {
        return uiContext.getValue(elementId)
                .map(Object::toString)
                .orElse("")
                .trim();
    }

    public static boolean readBooleanValue(UIContext uiContext, String elementId) {
        Object value = uiContext.getValue(elementId).orElse(false);
        if (value instanceof Boolean booleanValue) return booleanValue;

        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return false;
        }
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
