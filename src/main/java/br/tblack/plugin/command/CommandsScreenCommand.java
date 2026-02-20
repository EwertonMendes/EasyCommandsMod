package br.tblack.plugin.command;

import au.ellie.hyui.builders.CheckBoxBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TabNavigationBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.events.UIContext;
import au.ellie.hyui.html.TemplateProcessor;
import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
import br.tblack.plugin.enums.HudPositionPreset;
import br.tblack.plugin.hud.HUDEvent;
import br.tblack.plugin.hud.HudStore;
import br.tblack.plugin.i18n.Translations;
import br.tblack.plugin.util.EasyCommandsUtils;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.ArrayList;
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

    private static final String SLOTS_CONTENT_ID = "slots-content";
    private static final String CONFIG_CONTENT_ID = "config-content";

    private static final String TABS_NAV_ID = "easy-commands-tabs";

    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, Object> uiState = new HashMap<>();
    private final List<Map<String, Object>> slotsVm = new ArrayList<>();
    private final Map<Integer, Map<String, Object>> slotStateByNumber = new HashMap<>();

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

        hydrateStateFromStorage(playerUuid, playerRef);

        TemplateProcessor template = buildTemplate();

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef)
                .enableRuntimeTemplateUpdates(true)
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        registerTabStateTracking(pageBuilder);
        registerPerSlotInputNormalization(pageBuilder);
        registerPerSlotClearButtons(playerUuid, pageBuilder, playerRef, store);
        registerSaveAllButton(playerUuid, pageBuilder, playerRef, store);
        registerShowHudCheckbox(playerUuid, pageBuilder, playerRef, store);
        registerCloseButton(pageBuilder);
        registerHudPositionDropdown(pageBuilder, playerRef, store);
        registerLanguageDropdown(playerUuid, pageBuilder, playerRef, store);
        registerActivationModeDropdown(playerUuid, pageBuilder);

        pageBuilder.open(store);
    }

    private void hydrateStateFromStorage(UUID playerUuid, PlayerRef playerRef) {
        translations.clear();
        translations.putAll(buildI18n(playerUuid, playerRef));

        boolean showHud = PlayerConfig.isShowHud(playerUuid.toString());
        String hudPosition = PlayerConfig.getHudPosition(playerUuid.toString()).toString();
        String language = PlayerConfig.getLanguage(playerUuid.toString());
        String activationMode = PlayerConfig.getActivationMode(playerUuid.toString()).name();

        HudStore.setIsVisible(playerUuid, showHud);

        uiState.put("hudPosition", hudPosition);
        uiState.put("language", language);
        uiState.put("activationMode", activationMode);
        uiState.put("showHud", showHud);
        uiState.put("currentTab", "slots");

        slotsVm.clear();
        slotStateByNumber.clear();

        for (Integer slotNumber : COMMAND_SLOTS) {
            Map<String, Object> slotState = new HashMap<>();

            String value = ShortcutConfig.getCommand(playerUuid.toString(), slotNumber);
            if (value == null) value = "";

            slotState.put("slotNumber", slotNumber);
            slotState.put("inputId", getSlotInputId(slotNumber));
            slotState.put("clearId", getSlotClearButtonId(slotNumber));
            slotState.put("value", value);

            slotsVm.add(slotState);
            slotStateByNumber.put(slotNumber, slotState);
        }
    }

    private TemplateProcessor buildTemplate() {
        return new TemplateProcessor()
                .setVariable("translations", translations)
                .setVariable("slots", slotsVm)
                .setVariable("state", uiState);
    }

    private void registerTabStateTracking(PageBuilder pageBuilder) {
        pageBuilder.addEventListener(
                SLOTS_CONTENT_ID,
                CustomUIEventBindingType.Activating,
                (_, uiContext) -> uiState.put("currentTab", "slots")
        );

        pageBuilder.addEventListener(
                CONFIG_CONTENT_ID,
                CustomUIEventBindingType.Activating,
                (_, uiContext) -> uiState.put("currentTab", "config")
        );
    }

    private void registerPerSlotInputNormalization(PageBuilder pageBuilder) {
        for (Integer slotNumber : COMMAND_SLOTS) {
            String inputId = getSlotInputId(slotNumber);

            pageBuilder.addEventListener(
                    inputId,
                    CustomUIEventBindingType.ValueChanged,
                    (_, uiContext) -> {
                        String rawInputValue = EasyCommandsUtils.readStringValue(uiContext, inputId);
                        String normalizedCommand = EasyCommandsUtils.normalizeCommand(rawInputValue);

                        uiContext.getById(inputId, TextFieldBuilder.class)
                                .ifPresent(textField -> textField.withValue(normalizedCommand));

                        setSlotValueInTemplateState(slotNumber, normalizedCommand);
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

                        setSlotValueInTemplateState(slotNumber, "");

                        uiContext.updatePage(true);

                        HUDEvent.onCommandsChanged(playerRef, store);
                        syncShowHudCheckboxWithStore(playerUuid, pageBuilder);
                        playerRef.sendMessage(Translations.msg(playerUuid, "easycommands.msg.removedCommand", Color.ORANGE, "slotNumber", slotNumber));
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

                        for (Map.Entry<Integer, String> e : commandsBySlot.entrySet()) {
                            setSlotValueInTemplateState(e.getKey(), e.getValue());
                        }

                        uiContext.updatePage(true);

                        playerRef.sendMessage(Translations.msgSuccess(playerUuid, "easycommands.msg.commandsSavedSuccessfully"));

                    } catch (Exception e) {
                        playerRef.sendMessage(Translations.msgError(playerUuid, "easycommands.msg.errorSavingCommands"));
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
        uiState.put("showHud", HudStore.getIsVisible(playerUuid));

        pageBuilder.addEventListener(
                SHOW_HUD_CHECKBOX_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    boolean isHudVisible = EasyCommandsUtils.readBooleanValue(uiContext, SHOW_HUD_CHECKBOX_ID);

                    uiState.put("showHud", isHudVisible);

                    HUDEvent.setHudVisible(playerRef, store, isHudVisible);
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
                    String selectedHudPosition = EasyCommandsUtils.readStringValue(uiContext, HUD_POSITION_DROPDOWN_ID);
                    if (selectedHudPosition.isEmpty()) return;

                    HudPositionPreset hudPreset;
                    try {
                        hudPreset = HudPositionPreset.valueOf(selectedHudPosition);
                    } catch (Exception e) {
                        return;
                    }

                    uiState.put("hudPosition", selectedHudPosition);

                    HUDEvent.setHudPosition(playerRef, store, hudPreset);
                }
        );
    }

    private void registerLanguageDropdown(UUID playerUuid, PageBuilder pageBuilder, PlayerRef playerRef, Store<EntityStore> store) {
        pageBuilder.addEventListener(
                LANGUAGE_DROPDOWN_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, ctx) -> {
                    String selectedLanguage = EasyCommandsUtils.readStringValue(ctx, LANGUAGE_DROPDOWN_ID);
                    if (selectedLanguage.isEmpty()) return;

                    PlayerConfig.setLanguage(playerUuid.toString(), selectedLanguage);
                    Translations.reloadLanguage(selectedLanguage);

                    uiState.put("language", selectedLanguage);

                    translations.clear();
                    translations.putAll(buildI18n(playerUuid, playerRef));

                    ctx.getById(TABS_NAV_ID, TabNavigationBuilder.class).ifPresent(nav -> {
                        TabNavigationBuilder.Tab slotsTab = nav.getTab("slots");
                        if (slotsTab != null) {
                            TabNavigationBuilder.Tab updated = new TabNavigationBuilder.Tab(
                                    slotsTab.id(),
                                    translations.getOrDefault("slotsStr", slotsTab.label()),
                                    slotsTab.contentId(),
                                    slotsTab.selected(),
                                    slotsTab.buttonBuilder()
                            );
                            nav.updateTab("slots", updated);
                        }

                        TabNavigationBuilder.Tab configTab = nav.getTab("config");
                        if (configTab != null) {
                            TabNavigationBuilder.Tab updated = new TabNavigationBuilder.Tab(
                                    configTab.id(),
                                    translations.getOrDefault("configStr", configTab.label()),
                                    configTab.contentId(),
                                    configTab.selected(),
                                    configTab.buttonBuilder()
                            );
                            nav.updateTab("config", updated);
                        }
                    });

                    ctx.updatePage(true);

                    HUDEvent.onLanguageChanged(playerRef, store);
                }
        );
    }

    private void registerActivationModeDropdown(UUID playerUuid, PageBuilder pageBuilder) {
        pageBuilder.addEventListener(
                ACTIVATION_MODE_DROPDOWN_ID,
                CustomUIEventBindingType.ValueChanged,
                (_, uiContext) -> {
                    String selectedModeName = EasyCommandsUtils.readStringValue(uiContext, ACTIVATION_MODE_DROPDOWN_ID);

                    PlayerConfig.ActivationMode activationMode = parseActivationModeOrDefault(selectedModeName);
                    PlayerConfig.setActivationMode(playerUuid.toString(), activationMode);

                    uiState.put("activationMode", activationMode.name());
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

            String rawInputValue = EasyCommandsUtils.readStringValue(uiContext, inputId);
            String normalizedCommand = EasyCommandsUtils.normalizeCommand(rawInputValue);

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

    private void setSlotValueInTemplateState(int slotNumber, String newValue) {
        Map<String, Object> slotState = slotStateByNumber.get(slotNumber);
        if (slotState == null) return;

        slotState.put("value", newValue == null ? "" : newValue);
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

    private Map<String, String> buildI18n(UUID playerUuid, PlayerRef playerRef) {
        Map<String, String> i18n = new HashMap<>();

        i18n.put("title", Translations.msg(playerUuid, "easycommands.cmdScreen.title").getRawText());
        i18n.put("closeStr", Translations.msg(playerUuid, "easycommands.cmdScreen.close").getRawText());
        i18n.put("registeredCommandsFor", Translations.msg(
                playerUuid,
                "easycommands.cmdScreen.registeredCommandsFor",
                "playerName", playerRef.getUsername()
        ).getRawText());
        i18n.put("save", Translations.msg(playerUuid, "easycommands.cmdScreen.save").getRawText());
        i18n.put("slotStr", Translations.msg(playerUuid, "easycommands.slot").getRawText());
        i18n.put("slotsStr", Translations.msg(playerUuid, "easycommands.slots").getRawText());
        i18n.put("configStr", Translations.msg(playerUuid, "easycommands.config").getRawText());
        i18n.put("showHudStr", Translations.msg(playerUuid, "easycommands.cmdScreen.showHud").getRawText());
        i18n.put("hudPositionStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition").getRawText());
        i18n.put("topLeftStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.topLeft").getRawText());
        i18n.put("topRightStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.topRight").getRawText());
        i18n.put("centerLeftStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.centerLeft").getRawText());
        i18n.put("centerRightStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.centerRight").getRawText());
        i18n.put("bottomLeftStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.bottomLeft").getRawText());
        i18n.put("bottomRightStr", Translations.msg(playerUuid, "easycommands.cmdScreen.hudPosition.bottomRight").getRawText());
        i18n.put("languageStr", Translations.msg(playerUuid, "easycommands.cmdScreen.language").getRawText());
        i18n.put("activationModeStr", Translations.msg(playerUuid, "easycommands.cmdScreen.activationMode").getRawText());

        return i18n;
    }
}