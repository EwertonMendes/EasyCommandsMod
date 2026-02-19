package br.tblack.plugin.hud;

import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
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
        PlayerContext ctx = resolvePlayer(event);
        if (ctx == null) return;

        UUID uuid = ctx.player.getUuid();
        var settings = PlayerConfig.getForPlayer(uuid.toString());

        HudStore.setIsVisible(uuid, settings.showHud);
        HudStore.setPosition(uuid, settings.hudPosition);
        HudStore.clearDirty(uuid);

        if (settings.showHud) {
            rebuildHud(ctx.player, ctx.store);
        }
    }

    public static void onCommandsChanged(PlayerRef player, Store<EntityStore> store) {
        handleHudInvalidation(player, store);
    }

    public static void setHudVisible(PlayerRef player, Store<EntityStore> store, boolean visible) {
        UUID uuid = player.getUuid();

        HudStore.setIsVisible(uuid, visible);
        PlayerConfig.setShowHud(uuid.toString(), visible);

        if (!visible) {
            HyUIHud hud = HudStore.getHud(uuid);
            if (hud != null) hud.hide();
            HudStore.markDirty(uuid);
            return;
        }

        showHudEnsuringFresh(player, store);
    }

    public static void setHudPosition(PlayerRef player, Store<EntityStore> store, HudPositionPreset preset) {
        UUID uuid = player.getUuid();

        HudStore.setPosition(uuid, preset);
        PlayerConfig.setHudPosition(uuid.toString(), preset);

        handleHudInvalidation(player, store);
    }

    private static void handleHudInvalidation(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();

        if (!HudStore.getIsVisible(uuid)) {
            HudStore.markDirty(uuid);
            return;
        }

        rebuildHud(player, store);
    }

    private static void showHudEnsuringFresh(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();
        HyUIHud hud = HudStore.getHud(uuid);

        if (hud == null || HudStore.isDirty(uuid)) {
            rebuildHud(player, store);
            HudStore.clearDirty(uuid);
            return;
        }

        hud.unhide();
    }

    private static void rebuildHud(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();
        HudStore.removeHud(uuid);
        HyUIHud hud = buildHud(player, store);
        HudStore.setHud(uuid, hud);
    }

    private static HyUIHud buildHud(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();

        String hudStyle = HudStore.getPosition(uuid).getStyle();

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerCommands", getCommandsList(player))
                .setVariable("hudStyle", hudStyle);

        return HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html", template)
                .show(store);
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

    private static PlayerContext resolvePlayer(PlayerReadyEvent event) {
        Ref<EntityStore> ref = event.getPlayerRef();
        Store<EntityStore> store = ref.getStore();
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return null;
        return new PlayerContext(player, store);
    }

    private record PlayerContext(PlayerRef player, Store<EntityStore> store) {}
}
