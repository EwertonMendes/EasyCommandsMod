package br.tblack.plugin;

import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.enums.HudPositionPreset;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HUDEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        Store<EntityStore> store = playerRef.getStore();
        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());
        if (player == null) return;

        UUID uuid = player.getUuid();

        HudStore.setIsVisible(uuid, true);
        HudStore.clearDirty(uuid);

        createOrRecreateHud(player, store);
    }

    public static void onCommandsChanged(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();
        if (HudStore.getIsVisible(uuid)) {
            recreateHud(player, store);
        } else {
            HudStore.markDirty(uuid);
        }
    }

    public static void setHudVisible(PlayerRef player, Store<EntityStore> store, boolean visible) {
        UUID uuid = player.getUuid();
        HudStore.setIsVisible(uuid, visible);

        HyUIHud hud = HudStore.getHud(uuid);

        if (!visible) {
            if (hud != null) hud.hide();
            return;
        }

        if (hud == null || HudStore.isDirty(uuid)) {
            recreateHud(player, store);
            HudStore.clearDirty(uuid);
            return;
        }

        hud.unhide();
    }

    public static void setHudPosition(PlayerRef player, Store<EntityStore> store, HudPositionPreset preset) {
        UUID uuid = player.getUuid();
        HudStore.setPosition(uuid, preset);

        if (HudStore.getIsVisible(uuid)) {
            recreateHud(player, store);
        } else {
            HudStore.markDirty(uuid);
        }
    }

    private static void recreateHud(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();
        HudStore.removeHud(uuid);
        createOrRecreateHud(player, store);
    }

    private static void createOrRecreateHud(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();

        String hudStyle = HudStore.getPosition(uuid).getStyle();

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", getCommandsList(player))
                .setVariable("hudStyle", hudStyle);

        HyUIHud hud = HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html", template)
                .show(store);

        HudStore.setHud(uuid, hud);
    }

    private static List<Map<String, Object>> getCommandsList(PlayerRef player) {
        Map<Integer, String> playerCommands = ShortcutConfig.getForPlayer(player.getUuid().toString());

        return playerCommands.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("slot", entry.getKey());
                    m.put("command", entry.getValue());
                    return m;
                })
                .toList();
    }
}
