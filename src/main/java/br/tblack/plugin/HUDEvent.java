package br.tblack.plugin;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
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

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", getCommandsList(player));

        var hud = HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html", template)
                .show(store);

        HudStore.setHud(uuid, hud);
        HudStore.setIsVisible(uuid, true);
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

    public static void refreshPlayerCommandsHud(PlayerRef playerRef, Store<EntityStore> store) {

        UUID uuid = playerRef.getUuid();

        HudStore.removeHud(uuid);

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", getCommandsList(playerRef));

        var hud = HudBuilder.hudForPlayer(playerRef)
                .loadHtml("Huds/quick-command-hud.html", template)
                .show(store);

        HudStore.setHud(uuid, hud);
        HudStore.setIsVisible(uuid, true);
    }
}