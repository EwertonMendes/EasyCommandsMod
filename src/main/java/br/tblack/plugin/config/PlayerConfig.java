package br.tblack.plugin.config;

import br.tblack.plugin.enums.HudPositionPreset;
import br.tblack.plugin.i18n.Translations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlayerConfig {

    public enum ActivationMode {
        CTRL_F,
        O_ONLY
    }

    public static final String DEFAULT_LANGUAGE = "en-US";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR = "mods/EasyCommands";
    private static final String CONFIG_FILE = "players-configs.json";

    private static final Type DATA_TYPE = new TypeToken<Map<String, PlayerConfigData>>() {}.getType();

    private static final JsonConfigStore<Map<String, PlayerConfigData>> STORE =
            new JsonConfigStore<>(GSON, CONFIG_DIR, CONFIG_FILE, DATA_TYPE, HashMap::new);

    private static Map<String, PlayerConfigData> configByPlayerUuid = new HashMap<>();

    public static void load() {
        configByPlayerUuid = STORE.loadOrCreate();
        if (configByPlayerUuid == null) configByPlayerUuid = new HashMap<>();
    }

    public static void reload() {
        load();
    }

    public static void save() {
        STORE.save(configByPlayerUuid);
    }

    public static boolean hasPlayer(String uuid) {
        return uuid != null && !uuid.isBlank() && configByPlayerUuid.containsKey(uuid);
    }

    public static void clearCachedPlayer(String uuid) {
        if (uuid == null || uuid.isBlank()) return;
        configByPlayerUuid.remove(uuid);
    }

    public static PlayerConfigData getExistingOrNull(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return configByPlayerUuid.get(uuid);
    }

    public static PlayerConfigData initializeOrRepairForPlayer(String uuid, String detectedLanguage) {
        if (uuid == null || uuid.isBlank()) {
            PlayerConfigData fallback = PlayerConfigData.defaults(resolveLanguageOrDefault(detectedLanguage));
            fallback.normalizeNonLanguageFields();
            if (fallback.language == null || fallback.language.isBlank()) {
                fallback.language = resolveLanguageOrDefault(detectedLanguage);
            }
            return fallback;
        }

        PlayerConfigData existing = configByPlayerUuid.get(uuid);

        if (existing == null) {
            PlayerConfigData created = PlayerConfigData.defaults(resolveLanguageOrDefault(detectedLanguage));
            created.normalizeNonLanguageFields();
            if (created.language == null || created.language.isBlank()) {
                created.language = resolveLanguageOrDefault(detectedLanguage);
            }

            configByPlayerUuid.put(uuid, created);
            save();
            return created;
        }

        boolean changed = false;

        if (existing.hudPosition == null) {
            existing.hudPosition = HudPositionPreset.TOP_LEFT;
            changed = true;
        }

        if (existing.activationMode == null) {
            existing.activationMode = ActivationMode.CTRL_F;
            changed = true;
        }

        if (existing.language == null || existing.language.isBlank()) {
            existing.language = resolveLanguageOrDefault(detectedLanguage);
            changed = true;
        } else {
            String resolvedExistingLanguage = resolveLanguageOrDefault(existing.language);
            if (!resolvedExistingLanguage.equals(existing.language.trim())) {
                existing.language = resolvedExistingLanguage;
                changed = true;
            }
        }

        if (changed) {
            configByPlayerUuid.put(uuid, existing);
            save();
        }

        return existing;
    }

    public static PlayerConfigData getForPlayer(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            PlayerConfigData defaults = PlayerConfigData.defaults(DEFAULT_LANGUAGE);
            defaults.normalizeNonLanguageFields();
            if (defaults.language == null || defaults.language.isBlank()) {
                defaults.language = DEFAULT_LANGUAGE;
            }
            return defaults;
        }

        PlayerConfigData playerConfig = configByPlayerUuid.get(uuid);
        if (playerConfig == null) {
            PlayerConfigData defaults = PlayerConfigData.defaults(DEFAULT_LANGUAGE);
            defaults.normalizeNonLanguageFields();
            if (defaults.language == null || defaults.language.isBlank()) {
                defaults.language = DEFAULT_LANGUAGE;
            }
            return defaults;
        }

        playerConfig.normalizeNonLanguageFields();

        if (playerConfig.language == null || playerConfig.language.isBlank()) {
            playerConfig.language = DEFAULT_LANGUAGE;
        } else {
            playerConfig.language = resolveLanguageOrDefault(playerConfig.language);
        }

        return playerConfig;
    }

    public static void setShowHud(String uuid, boolean showHud) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = hasPlayer(uuid)
                ? getForPlayer(uuid)
                : initializeOrRepairForPlayer(uuid, DEFAULT_LANGUAGE);

        playerConfig.showHud = showHud;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static boolean isShowHud(String uuid) {
        return getForPlayer(uuid).showHud;
    }

    public static void setHudPosition(String uuid, HudPositionPreset preset) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = hasPlayer(uuid)
                ? getForPlayer(uuid)
                : initializeOrRepairForPlayer(uuid, DEFAULT_LANGUAGE);

        playerConfig.hudPosition = preset;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static HudPositionPreset getHudPosition(String uuid) {
        return getForPlayer(uuid).hudPosition;
    }

    public static void setLanguage(String uuid, String language) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = hasPlayer(uuid)
                ? getForPlayer(uuid)
                : initializeOrRepairForPlayer(uuid, language);

        playerConfig.language = resolveLanguageOrDefault(language);
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static String getLanguage(String uuid) {
        return getForPlayer(uuid).language;
    }

    public static void setActivationMode(String uuid, ActivationMode mode) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = hasPlayer(uuid)
                ? getForPlayer(uuid)
                : initializeOrRepairForPlayer(uuid, DEFAULT_LANGUAGE);

        playerConfig.activationMode = (mode == null) ? ActivationMode.CTRL_F : mode;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static ActivationMode getActivationMode(String uuid) {
        return getForPlayer(uuid).activationMode;
    }

    private static String resolveLanguageOrDefault(String language) {
        return Translations.resolveSupportedLanguageOrDefault(language);
    }

    public static class PlayerConfigData {
        public boolean showHud;
        public HudPositionPreset hudPosition;
        public String language;
        public ActivationMode activationMode;

        public static PlayerConfigData defaults(String language) {
            PlayerConfigData defaults = new PlayerConfigData();
            defaults.showHud = true;
            defaults.hudPosition = HudPositionPreset.TOP_LEFT;
            defaults.language = language;
            defaults.activationMode = ActivationMode.CTRL_F;
            return defaults;
        }

        public void normalizeNonLanguageFields() {
            if (hudPosition == null) hudPosition = HudPositionPreset.TOP_LEFT;
            if (activationMode == null) activationMode = ActivationMode.CTRL_F;
        }
    }
}