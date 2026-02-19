package br.tblack.plugin.command;

import au.ellie.hyui.builders.CheckBoxBuilder;
import au.ellie.hyui.builders.DropdownBoxBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.events.UIContext;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.hud.HUDEvent;
import br.tblack.plugin.hud.HudStore;
import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
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

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandsScreenCommand extends AbstractPlayerCommand {

    private static final List<Integer> COMMAND_SLOTS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    private static final String SAVE_ALL_BUTTON_ID = "save-all-button";
    private static final String CLOSE_BUTTON_ID = "close-button";
    private static final String SHOW_HUD_CHECKBOX_ID = "show-hud-checkbox";
    private static final String HUD_POSITION_DROPDOWN_ID = "hud-position";
    private static final String LANGUAGE_DROPDOWN_ID = "language";
    private static final String ACTIVATION_MODE_DROPDOWN_ID = "activation-mode";

    public CommandsScreenCommand() {
        super("cmd", "Shows a screen with all available slots for registering commands to run via shortcuts.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        UUID playerUuid = commandContext.sender().getUuid();

        TemplateProcessor template = buildTemplate(playerUuid, playerRef);

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        loadInitialValues(playerUuid, pageBuilder);
        registerPerSlotInputNormalization(pageBuilder);
        registerPerSlotClearButtons(playerUuid, pageBuilder, playerRef, store);
        registerSaveAllButton(playerUuid, pageBuilder, playerRef, store);
        registerShowHudCheckbox(playerUuid, pageBuilder, playerRef, store);
        registerCloseButton(pageBuilder);
        registerHudPositionDropdown(pageBuilder, playerRef, store);
        registerLanguageDropdown(pageBuilder, playerRef);
        registerActivationModeDropdown(pageBuilder, playerRef);

        pageBuilder.open(store);
    }

    private TemplateProcessor buildTemplate(UUID playerUuid, PlayerRef playerRef) {
        String hudPosition = PlayerConfig.getHudPosition(playerRef.getUuid().toString()).toString();
        String language = PlayerConfig.getLanguage(playerUuid.toString());
        String activationMode = PlayerConfig.getActivationMode(playerUuid.toString()).name();

        return new TemplateProcessor()
                .setVariable("playerName", playerRef.getUsername())
                .setVariable("slots", COMMAND_SLOTS)
                .setVariable("hudPosition", hudPosition)
                .setVariable("language", language)
                .setVariable("activationMode", activationMode);
    }

    private void registerPerSlotInputNormalization(PageBuilder pageBuilder) {
        for (Integer slotNumber : COMMAND_SLOTS) {
            String inputId = getSlotInputId(slotNumber);

            pageBuilder.addEventListener(
                    inputId,
                    CustomUIEventBindingType.ValueChanged,
                    (_, uiContext) -> {
                        String rawInputValue = readStringValue(uiContext, inputId);
                        String normalizedCommand = normalizeCommand(rawInputValue);

                        uiContext.getById(inputId, TextFieldBuilder.class)
                                .ifPresent(textField -> textField.withValue(normalizedCommand));
                    }
            );
        }
    }

    private void registerPerSlotClearButtons(
            UUID playerUuid,
            PageBuilder pageBuilder,
            PlayerRef playerRef,
            Store<EntityStore> store
    ) {
        for (Integer slotNumber : COMMAND_SLOTS) {
            String clearButtonId = getSlotClearButtonId(slotNumber);
            String inputId = getSlotInputId(slotNumber);

            pageBuilder.addEventListener(
                    clearButtonId,
                    CustomUIEventBindingType.Activating,
                    (_, uiContext) -> {
                        ShortcutConfig.removeCommand(playerUuid.toString(), slotNumber);

                        uiContext.getById(inputId, TextFieldBuilder.class)
                                .ifPresent(textField -> textField.withValue(""));

                        uiContext.updatePage(true);

                        HUDEvent.onCommandsChanged(playerRef, store);
                        syncShowHudCheckboxWithStore(playerUuid, pageBuilder);

                        playerRef.sendMessage(
                                Message.raw("Removed command from slot " + slotNumber + " successfully!")
                                        .color(Color.ORANGE)
                        );
                    }
            );
        }
    }

    private void registerSaveAllButton(
            UUID playerUuid,
            PageBuilder pageBuilder,
            PlayerRef playerRef,
            Store<EntityStore> store
    ) {
        pageBuilder.addEventListener(
                SAVE_ALL_BUTTON_ID,
                CustomUIEventBindingType.Activating,
                (_, uiContext) -> {
                    try {
                        Map<Integer, String> commandsBySlot = collectAllSlotCommands(uiContext);
                        persistCommands(playerUuid, commandsBySlot);

                        uiContext.updatePage(true);

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
                        syncShowHudCheckboxWithStore(playerUuid, pageBuilder);
                    }
                }
        );
    }

    private void registerShowHudCheckbox(
            UUID playerUuid,
            PageBuilder pageBuilder,
            PlayerRef playerRef,
            Store<EntityStore> store
    ) {
        HudStore.setIsVisible(playerUuid, PlayerConfig.isShowHud(playerUuid.toString()));
        syncShowHudCheckboxWithStore(playerUuid, pageBuilder);

        pageBuilder.addEventListener(
                SHOW_HUD_CHECKBOX_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    boolean isHudVisible = readBooleanValue(uiContext, SHOW_HUD_CHECKBOX_ID);

                    HUDEvent.setHudVisible(playerRef, store, isHudVisible);
                    syncShowHudCheckboxWithStore(playerUuid, pageBuilder);
                }
        );
    }

    private void registerCloseButton(PageBuilder pageBuilder) {
        pageBuilder.addEventListener(
                CLOSE_BUTTON_ID,
                CustomUIEventBindingType.Activating,
                (_, uiContext) -> uiContext.getPage().ifPresent(HyUIPage::close)
        );
    }

    private void registerHudPositionDropdown(PageBuilder pageBuilder, PlayerRef playerRef, Store<EntityStore> store) {
        pageBuilder.addEventListener(
                HUD_POSITION_DROPDOWN_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    String selectedHudPosition = readStringValue(uiContext, HUD_POSITION_DROPDOWN_ID);
                    if (selectedHudPosition.isEmpty()) return;

                    HudPositionPreset hudPreset;
                    try {
                        hudPreset = HudPositionPreset.valueOf(selectedHudPosition);
                    } catch (Exception e) {
                        return;
                    }

                    HUDEvent.setHudPosition(playerRef, store, hudPreset);
                }
        );
    }

    private void registerLanguageDropdown(PageBuilder pageBuilder, PlayerRef playerRef) {
        pageBuilder.addEventListener(
                LANGUAGE_DROPDOWN_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    String selectedLanguage = readStringValue(uiContext, LANGUAGE_DROPDOWN_ID);
                    if (selectedLanguage.isEmpty()) return;

                    PlayerConfig.setLanguage(playerRef.getUuid().toString(), selectedLanguage);
                }
        );
    }

    private void registerActivationModeDropdown(PageBuilder pageBuilder, PlayerRef playerRef) {
        pageBuilder.addEventListener(
                ACTIVATION_MODE_DROPDOWN_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    String selectedModeName = readStringValue(uiContext, ACTIVATION_MODE_DROPDOWN_ID);

                    PlayerConfig.ActivationMode activationMode = parseActivationModeOrDefault(selectedModeName);
                    PlayerConfig.setActivationMode(playerRef.getUuid().toString(), activationMode);
                }
        );
    }

    private PlayerConfig.ActivationMode parseActivationModeOrDefault(String modeName) {
        if (modeName == null || modeName.isBlank()) return PlayerConfig.ActivationMode.CTRL_F;

        try {
            return PlayerConfig.ActivationMode.valueOf(modeName);
        } catch (Exception e) {
            return PlayerConfig.ActivationMode.CTRL_F;
        }
    }

    private Map<Integer, String> collectAllSlotCommands(UIContext uiContext) {
        Map<Integer, String> commandsBySlot = new HashMap<>();

        for (Integer slotNumber : COMMAND_SLOTS) {
            String inputId = getSlotInputId(slotNumber);

            String rawInputValue = readStringValue(uiContext, inputId);
            String normalizedCommand = normalizeCommand(rawInputValue);

            commandsBySlot.put(slotNumber, normalizedCommand);
        }

        return commandsBySlot;
    }

    private void persistCommands(UUID playerUuid, Map<Integer, String> commandsBySlot) {
        for (Map.Entry<Integer, String> entry : commandsBySlot.entrySet()) {
            Integer slotNumber = entry.getKey();
            String command = entry.getValue();

            if (command.isEmpty()) {
                ShortcutConfig.removeCommand(playerUuid.toString(), slotNumber);
                continue;
            }

            ShortcutConfig.setCommand(playerUuid.toString(), slotNumber, command);
        }
    }

    private void loadInitialValues(UUID playerUuid, PageBuilder pageBuilder) {
        for (Integer slotNumber : COMMAND_SLOTS) {
            pageBuilder.getById(getSlotInputId(slotNumber), TextFieldBuilder.class)
                    .ifPresent(textField -> textField.withValue(ShortcutConfig.getCommand(playerUuid.toString(), slotNumber)));
        }

        pageBuilder.getById(SHOW_HUD_CHECKBOX_ID, CheckBoxBuilder.class)
                .ifPresent(checkBox -> checkBox.withValue(PlayerConfig.isShowHud(playerUuid.toString())));

        pageBuilder.getById(HUD_POSITION_DROPDOWN_ID, DropdownBoxBuilder.class)
                .ifPresent(dropdown -> dropdown.withValue(PlayerConfig.getHudPosition(playerUuid.toString()).toString()));

        pageBuilder.getById(LANGUAGE_DROPDOWN_ID, DropdownBoxBuilder.class)
                .ifPresent(dropdown -> dropdown.withValue(PlayerConfig.getLanguage(playerUuid.toString())));

        pageBuilder.getById(ACTIVATION_MODE_DROPDOWN_ID, DropdownBoxBuilder.class)
                .ifPresent(dropdown -> dropdown.withValue(PlayerConfig.getActivationMode(playerUuid.toString()).name()));
    }

    private String normalizeCommand(String commandInput) {
        if (commandInput == null) return "";

        String normalized = commandInput.trim();
        if (normalized.isEmpty()) return "";

        if (normalized.startsWith("/")) normalized = normalized.substring(1).trim();
        return normalized;
    }

    private String readStringValue(UIContext uiContext, String elementId) {
        return uiContext.getValue(elementId)
                .map(Object::toString)
                .orElse("")
                .trim();
    }

    private boolean readBooleanValue(UIContext uiContext, String elementId) {
        Object value = uiContext.getValue(elementId).orElse(false);
        if (value instanceof Boolean booleanValue) return booleanValue;

        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private String getSlotInputId(int slotNumber) {
        return "slot-" + slotNumber + "-input";
    }

    private String getSlotClearButtonId(int slotNumber) {
        return "slot-" + slotNumber + "-button-clear";
    }

    private void syncShowHudCheckboxWithStore(UUID playerUuid, PageBuilder pageBuilder) {
        pageBuilder.getById(SHOW_HUD_CHECKBOX_ID, CheckBoxBuilder.class)
                .ifPresent(checkBox -> checkBox.withValue(HudStore.getIsVisible(playerUuid)));
    }
}
