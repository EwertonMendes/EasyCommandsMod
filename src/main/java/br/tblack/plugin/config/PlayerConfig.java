package br.tblack.plugin.config;

import br.tblack.plugin.enums.HudPositionPreset;
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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR = "plugins/EasyCommands";
    private static final String CONFIG_FILE = "players-configs.json";

    private static final Type DATA_TYPE = new TypeToken<Map<String, PlayerConfigData>>() {}.getType();

    private static final JsonConfigStore<Map<String, PlayerConfigData>> STORE =
            new JsonConfigStore<>(GSON, CONFIG_DIR, CONFIG_FILE, DATA_TYPE, HashMap::new);

    private static Map<String, PlayerConfigData> configByPlayerUuid = new HashMap<>();

    public static void load() {
        configByPlayerUuid = STORE.loadOrCreate();
        if (configByPlayerUuid == null) configByPlayerUuid = new HashMap<>();
    }

    public static void save() {
        STORE.save(configByPlayerUuid);
    }

    public static PlayerConfigData getForPlayer(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            PlayerConfigData defaults = PlayerConfigData.defaults();
            defaults.normalize();
            return defaults;
        }

        PlayerConfigData playerConfig = configByPlayerUuid.get(uuid);
        if (playerConfig == null) {
            playerConfig = PlayerConfigData.defaults();
            configByPlayerUuid.put(uuid, playerConfig);
            save();
        }

        playerConfig.normalize();
        return playerConfig;
    }

    public static void setShowHud(String uuid, boolean showHud) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = getForPlayer(uuid);
        playerConfig.showHud = showHud;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static boolean isShowHud(String uuid) {
        return getForPlayer(uuid).showHud;
    }

    public static void setHudPosition(String uuid, HudPositionPreset preset) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = getForPlayer(uuid);
        playerConfig.hudPosition = preset;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static HudPositionPreset getHudPosition(String uuid) {
        return getForPlayer(uuid).hudPosition;
    }

    public static void setLanguage(String uuid, String language) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = getForPlayer(uuid);
        playerConfig.language = (language == null) ? null : language.trim();
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static String getLanguage(String uuid) {
        return getForPlayer(uuid).language;
    }

    public static void setActivationMode(String uuid, ActivationMode mode) {
        if (uuid == null || uuid.isBlank()) return;

        PlayerConfigData playerConfig = getForPlayer(uuid);
        playerConfig.activationMode = (mode == null) ? ActivationMode.CTRL_F : mode;
        configByPlayerUuid.put(uuid, playerConfig);
        save();
    }

    public static ActivationMode getActivationMode(String uuid) {
        return getForPlayer(uuid).activationMode;
    }

    public static class PlayerConfigData {
        public boolean showHud;
        public HudPositionPreset hudPosition;
        public String language;
        public ActivationMode activationMode;

        public static PlayerConfigData defaults() {
            PlayerConfigData defaults = new PlayerConfigData();
            defaults.showHud = true;
            defaults.hudPosition = HudPositionPreset.TOP_LEFT;
            defaults.language = "en_US";
            defaults.activationMode = ActivationMode.CTRL_F;
            return defaults;
        }

        public void normalize() {
            if (hudPosition == null) hudPosition = HudPositionPreset.TOP_LEFT;
            if (language == null || language.trim().isEmpty()) language = "en_US";
            if (activationMode == null) activationMode = ActivationMode.CTRL_F;
        }
    }
}
