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

public class HUDEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {

        Ref<EntityStore> playerRef = event.getPlayerRef();
        Store<EntityStore> store = playerRef.getStore();

        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());

        assert player != null;
        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", getCommandsList(player));

        HudBuilder hudBuilder = HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html", template);

        var hud = hudBuilder.show(store);

        HudStore.setHud(hud);
        HudStore.setIsVisible(true);
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


    public static void refreshPlayerCommandsHud(PlayerRef playerRef, Store<EntityStore> store ) {
        var currentHud = HudStore.getHud();

        if (currentHud == null) return;

        currentHud.remove();

        List<Map<String, Object>> commandsList = getCommandsList(playerRef);

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", commandsList);

        HudBuilder updatedHud = HudBuilder.hudForPlayer(playerRef)
                .loadHtml("Huds/quick-command-hud.html", template);

        var hud = updatedHud.show(store);

        HudStore.setHud(hud);
        HudStore.setIsVisible(true);
    }

}
