package br.tblack.plugin;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;

public class EasyCommandsUtils {

    public static String normalize(String s) {
        String t = s.trim();
        if (t.startsWith("/")) t = t.substring(1).trim();
        return t.toLowerCase(Locale.ROOT);
    }

    public static String tryReadChatMessage(Packet packet) {
        Object value = tryReadField(packet, "message");
        return (value instanceof String str) ? str : null;
    }

    public static Object tryReadField(Object target, String fieldName) {
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
