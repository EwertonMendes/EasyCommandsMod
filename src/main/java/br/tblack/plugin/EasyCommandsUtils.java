package br.tblack.plugin;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Locale;
import java.util.function.Consumer;

public class EasyCommandsUtils {

    public static String normalizeCommand(String s) {
        String t = s.trim();
        if (t.startsWith("/")) t = t.substring(1).trim();
        return t.toLowerCase(Locale.ROOT);
    }

    public static void runOnWorldThread(PlayerRef playerApiRef, Consumer<Player> action) {
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
