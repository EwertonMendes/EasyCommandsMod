package br.tblack.plugin.hud;

import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
import br.tblack.plugin.enums.HudPositionPreset;
import br.tblack.plugin.i18n.Translations;
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

    private static final String ROW_BG_NORMAL = "rgba(40, 120, 180, 0.14)";
    private static final String ROW_BG_DIM = "rgba(40, 120, 180, 0.03)";
    private static final String ROW_BG_ACTIVE = "rgba(40, 120, 180, 0.62)";

    private static final String SLOT_BG_INACTIVE = "rgba(0, 220, 255, 0.18)";
    private static final String SLOT_BG_ACTIVE = "rgba(0, 220, 255, 1.00)";

    private static final String TEXT_NORMAL = "rgba(255, 255, 255, 0.92)";
    private static final String TEXT_DIM = "rgba(255, 255, 255, 0.40)";
    private static final String TEXT_ACTIVE = "rgba(255, 255, 255, 1.00)";

    private static final int CMD_SIZE_NORMAL = 14;
    private static final int CMD_SIZE_ACTIVE = 18;

    private static final int SLOT_SIZE_NORMAL = 14;
    private static final int SLOT_SIZE_ACTIVE = 18;

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

    public static void onLanguageChanged(PlayerRef player, Store<EntityStore> store) {
        UUID uuid = player.getUuid();

        if (!HudStore.getIsVisible(uuid)) {
            HudStore.markDirty(uuid);
            return;
        }

        rebuildHud(player, store);
        HudStore.clearDirty(uuid);
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
                .setVariable("title", Translations.msg(uuid, "easycommands.hud.title").getRawText())
                .setVariable("noCommandMsg1", Translations.msg(uuid, "easycommands.hud.noCommandMsg1").getRawText())
                .setVariable("noCommandMsg2", Translations.msg(uuid, "easycommands.hud.noCommandMsg2").getRawText())
                .setVariable("playerCommands", getCommandsList(uuid))
                .setVariable("hudStyle", hudStyle);

        return HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html", template)
                .show(store);
    }

    private static List<Map<String, Object>> getCommandsList(UUID uuid) {
        Map<Integer, String> commands = ShortcutConfig.getForPlayer(uuid.toString());

        int highlighted = HudStore.getHighlightedSlot(uuid);
        boolean hasHighlight = highlighted != -1 && commands.containsKey(highlighted);

        return commands.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    int slot = entry.getKey();
                    boolean isActive = hasHighlight && slot == highlighted;
                    boolean isDim = hasHighlight && slot != highlighted;

                    String rowBg = isActive ? ROW_BG_ACTIVE : (isDim ? ROW_BG_DIM : ROW_BG_NORMAL);

                    String slotBg = isActive ? SLOT_BG_ACTIVE : SLOT_BG_INACTIVE;

                    String cmdTextColor = isActive ? TEXT_ACTIVE : (isDim ? TEXT_DIM : TEXT_NORMAL);
                    String slotTextColor = isActive ? TEXT_ACTIVE : (isDim ? TEXT_DIM : TEXT_NORMAL);

                    int cmdSize = isActive ? CMD_SIZE_ACTIVE : CMD_SIZE_NORMAL;
                    String cmdWeight = isActive ? "bold" : "normal";

                    int slotSize = isActive ? SLOT_SIZE_ACTIVE : SLOT_SIZE_NORMAL;

                    Map<String, Object> m = new HashMap<>();
                    m.put("slot", slot);
                    m.put("command", entry.getValue());

                    m.put("rowBg", rowBg);
                    m.put("slotBg", slotBg);

                    m.put("cmdFontSize", cmdSize);
                    m.put("cmdFontWeight", cmdWeight);
                    m.put("slotFontSize", slotSize);

                    m.put("cmdTextColor", cmdTextColor);
                    m.put("slotTextColor", slotTextColor);

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