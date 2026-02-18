package br.tblack.plugin;

import br.tblack.plugin.enums.HudPositionPreset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PlayerConfig {

    public enum ActivationMode {
        CTRL_F,
        O_ONLY
    }

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR = "plugins/EasyCommands";
    private static final String CONFIG_FILE = "players-configs.json";

    private static final Type TYPE = new TypeToken<Map<String, PlayerConfigData>>() {}.getType();

    private static Map<String, PlayerConfigData> perPlayerConfig = new HashMap<>();

    public static void load() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, CONFIG_FILE);
            if (!file.exists()) {
                perPlayerConfig = new HashMap<>();
                save();
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                Map<String, PlayerConfigData> loaded = gson.fromJson(reader, TYPE);
                perPlayerConfig = (loaded != null) ? loaded : new HashMap<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            perPlayerConfig = new HashMap<>();
        }
    }

    public static void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, CONFIG_FILE);
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(perPlayerConfig, TYPE, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PlayerConfigData getForPlayer(String uuid) {
        PlayerConfigData cfg = perPlayerConfig.get(uuid);
        if (cfg == null) {
            cfg = PlayerConfigData.defaults();
            perPlayerConfig.put(uuid, cfg);
            save();
        }
        cfg.normalize();
        return cfg;
    }

    public static void setShowHud(String uuid, boolean showHud) {
        PlayerConfigData cfg = getForPlayer(uuid);
        cfg.showHud = showHud;
        perPlayerConfig.put(uuid, cfg);
        save();
    }

    public static boolean isShowHud(String uuid) {
        return getForPlayer(uuid).showHud;
    }

    public static void setHudPosition(String uuid, HudPositionPreset preset) {
        PlayerConfigData cfg = getForPlayer(uuid);
        cfg.hudPosition = preset;
        perPlayerConfig.put(uuid, cfg);
        save();
    }

    public static HudPositionPreset getHudPosition(String uuid) {
        return getForPlayer(uuid).hudPosition;
    }

    public static void setLanguage(String uuid, String language) {
        PlayerConfigData cfg = getForPlayer(uuid);
        cfg.language = (language == null) ? null : language.trim();
        perPlayerConfig.put(uuid, cfg);
        save();
    }

    public static String getLanguage(String uuid) {
        return getForPlayer(uuid).language;
    }

    public static void setActivationMode(String uuid, ActivationMode mode) {
        PlayerConfigData cfg = getForPlayer(uuid);
        cfg.activationMode = (mode == null) ? ActivationMode.CTRL_F : mode;
        perPlayerConfig.put(uuid, cfg);
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
            PlayerConfigData d = new PlayerConfigData();
            d.showHud = true;
            d.hudPosition = HudPositionPreset.TOP_RIGHT;
            d.language = "en_US";
            d.activationMode = ActivationMode.CTRL_F;
            return d;
        }

        public void normalize() {
            if (hudPosition == null) hudPosition = HudPositionPreset.TOP_RIGHT;
            if (language == null || language.trim().isEmpty()) language = "en_US";
            if (activationMode == null) activationMode = ActivationMode.CTRL_F;
        }
    }
}
