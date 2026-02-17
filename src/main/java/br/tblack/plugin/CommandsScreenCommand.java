package br.tblack.plugin;

import au.ellie.hyui.builders.CheckBoxBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.events.UIContext;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.enums.HudPositionPreset;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandsScreenCommand extends AbstractPlayerCommand {

    private static final List<Integer> SLOTS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    public CommandsScreenCommand() {
        super("cmd", "Shows a screen with all available slots for registering commands to run via shortcuts.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {

        UUID uuid = context.sender().getUuid();

        String hudPosition = HudStore.getPosition(playerRef.getUuid()).toString();

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerName", playerRef.getUsername())
                .setVariable("slots", SLOTS)
                .setVariable("hudPosition", hudPosition);;

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        loadInitialValues(uuid, pageBuilder);
        registerInputListeners(uuid, pageBuilder);
        registerClearListeners(uuid, pageBuilder, playerRef, store);
        registerSaveListener(uuid, pageBuilder, playerRef, store);
        registerShowHudCheckboxListener(uuid, pageBuilder, playerRef, store);
        registerCloseButtonListener(pageBuilder);
        registerHudSelectionListener(playerRef, store, pageBuilder);

        pageBuilder.open(store);
    }

    private void registerInputListeners(UUID uuid, PageBuilder pageBuilder) {
        for (Integer slot : SLOTS) {
            pageBuilder.addEventListener(
                    getInputId(slot),
                    CustomUIEventBindingType.ValueChanged,
                    (event, ctx) -> {
                        String value = ctx.getValue(getInputId(slot))
                                .map(Object::toString)
                                .orElse("");

                        String normalized = normalizeCommand(value);

                        ctx.getById(getInputId(slot), TextFieldBuilder.class)
                                .ifPresent(tf -> tf.withValue(normalized));
                    }
            );
        }
    }

    private void registerClearListeners(UUID uuid,
                                        PageBuilder pageBuilder,
                                        PlayerRef playerRef,
                                        Store<EntityStore> store) {

        for (Integer slot : SLOTS) {
            pageBuilder.addEventListener(
                    getClearButtonId(slot),
                    CustomUIEventBindingType.Activating,
                    (_, ctx) -> {

                        ShortcutConfig.removeCommand(uuid.toString(), slot);

                        ctx.getById(getInputId(slot), TextFieldBuilder.class)
                                .ifPresent(tf -> tf.withValue(""));

                        ctx.updatePage(true);

                        HUDEvent.onCommandsChanged(playerRef, store);
                        updateCheckBoxValue(uuid, pageBuilder);

                        playerRef.sendMessage(
                                Message.raw("Removed command from slot " + slot + " successfully!")
                                        .color(Color.ORANGE)
                        );
                    }
            );
        }
    }

    private void registerSaveListener(UUID uuid,
                                      PageBuilder pageBuilder,
                                      PlayerRef playerRef,
                                      Store<EntityStore> store) {

        pageBuilder.addEventListener(
                "save-all-button",
                CustomUIEventBindingType.Activating,
                (_, ctx) -> {

                    try {
                        Map<Integer, String> commands = collectAllInputValues(ctx);
                        persistCommands(uuid, commands);
                        ctx.updatePage(true);

                        playerRef.sendMessage(
                                Message.raw("Commands saved successfully!")
                                        .color(Color.GREEN)
                        );

                    } catch (Exception e) {

                        playerRef.sendMessage(
                                Message.raw("Error saving commands, try again later!")
                                        .color(Color.RED)
                        );
                    } finally {
                        HUDEvent.onCommandsChanged(playerRef, store);
                        updateCheckBoxValue(uuid, pageBuilder);
                    }
                }
        );
    }

    private void registerShowHudCheckboxListener(UUID uuid,
                                                 PageBuilder pageBuilder,
                                                 PlayerRef playerRef,
                                                 Store<EntityStore> store) {
        updateCheckBoxValue(uuid, pageBuilder);
        pageBuilder.addEventListener(
                "show-hud-checkbox",
                CustomUIEventBindingType.ValueChanged,
                (_, ctx) -> {
                    boolean checkBoxValue = ctx.getValue("show-hud-checkbox")
                            .map(Boolean.class::cast)
                            .orElse(false);

                    HUDEvent.setHudVisible(playerRef, store, checkBoxValue);
                    updateCheckBoxValue(uuid, pageBuilder);
                }
        );
    }

    private void registerCloseButtonListener(PageBuilder pageBuilder) {
        pageBuilder.addEventListener("close-button", CustomUIEventBindingType.Activating, (_, ctx) -> {
            ctx.getPage().ifPresent(HyUIPage::close);
        });
    }

    private void registerHudSelectionListener(PlayerRef playerRef, Store<EntityStore> store, PageBuilder pageBuilder) {
        pageBuilder.addEventListener("hud-position", CustomUIEventBindingType.ValueChanged, (_, ctx) -> {
            var selectedHudPosition = ctx.getValue("hud-position").map(Object::toString)
                    .orElse("");

            HUDEvent.setHudPosition(playerRef, store, HudPositionPreset.valueOf(selectedHudPosition));
        });
    }

    private Map<Integer, String> collectAllInputValues(Object ctx) {
        Map<Integer, String> values = new HashMap<>();

        for (Integer slot : SLOTS) {
            String raw = ((UIContext) ctx)
                    .getValue(getInputId(slot))
                    .map(Object::toString)
                    .orElse("");

            values.put(slot, normalizeCommand(raw));
        }

        return values;
    }

    private void persistCommands(UUID uuid, Map<Integer, String> commands) {
        for (Map.Entry<Integer, String> entry : commands.entrySet()) {

            Integer slot = entry.getKey();
            String command = entry.getValue();

            if (command.isEmpty()) {
                ShortcutConfig.removeCommand(uuid.toString(), slot);
                continue;
            }

            ShortcutConfig.setCommand(uuid.toString(), slot, command);
        }
    }

    private void loadInitialValues(UUID uuid, PageBuilder pageBuilder) {
        for (Integer slot : SLOTS) {
            pageBuilder.getById(getInputId(slot), TextFieldBuilder.class)
                    .ifPresent(tf -> tf.withValue(ShortcutConfig.getCommand(uuid.toString(), slot)));
        }
    }

    private String normalizeCommand(String input) {
        if (input == null) return "";

        String value = input.trim();

        if (value.startsWith("/")) {
            value = value.substring(1);
        }

        return value.trim();
    }

    private String getInputId(int slot) {
        return "slot-" + slot + "-input";
    }

    private String getClearButtonId(int slot) {
        return "slot-" + slot + "-button-clear";
    }

    private void updateCheckBoxValue(UUID uuid, PageBuilder pageBuilder) {
        pageBuilder.getById("show-hud-checkbox", CheckBoxBuilder.class)
                .ifPresent(cb -> cb.withValue(HudStore.getIsVisible(uuid)));
    }
}