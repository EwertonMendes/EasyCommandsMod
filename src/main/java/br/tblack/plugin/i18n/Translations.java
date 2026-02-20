package br.tblack.plugin.i18n;

import br.tblack.plugin.config.PlayerConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.LocalizableString;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;

public final class Translations {

    public static final Color SUCCESS = Color.GREEN;
    public static final Color ERROR = Color.RED;
    public static final Color WARNING = new Color(255, 200, 0);
    public static final Color INFO = new Color(128, 128, 128);
    public static final Color HIGHLIGHT = new Color(85, 255, 255);

    private Translations() {}

    public static void init() {
        TranslationManager.getInstance();
    }

    private static String lang(UUID playerUuid) {
        String l = PlayerConfig.getLanguage(playerUuid.toString());
        if (l == null || l.isBlank()) return "en-US";
        return l.trim();
    }

    public static String tr(UUID playerUuid, String key, Object... params) {
        return TranslationManager.getInstance().get(lang(playerUuid), key, params);
    }

    public static String tr(UUID playerUuid, String key, Map<String, Object> params) {
        return TranslationManager.getInstance().get(lang(playerUuid), key, params);
    }

    public static Message msg(UUID playerUuid, String key, Object... params) {
        return TranslationManager.getInstance().message(lang(playerUuid), key, params);
    }

    public static Message msg(UUID playerUuid, String key, Color color, Object... params) {
        return TranslationManager.getInstance().message(lang(playerUuid), key, color, params);
    }

    public static Message msgSuccess(UUID playerUuid, String key, Object... params) {
        return msg(playerUuid, key, SUCCESS, params);
    }

    public static Message msgError(UUID playerUuid, String key, Object... params) {
        return msg(playerUuid, key, ERROR, params);
    }

    public static Message msgWarning(UUID playerUuid, String key, Object... params) {
        return msg(playerUuid, key, WARNING, params);
    }

    public static Message msgInfo(UUID playerUuid, String key, Object... params) {
        return msg(playerUuid, key, INFO, params);
    }

    public static LocalizableString loc(UUID playerUuid, String key, Object... params) {
        return TranslationManager.getInstance().localizable(lang(playerUuid), key, params);
    }

    public static boolean hasKey(UUID playerUuid, String key) {
        return TranslationManager.getInstance().hasKey(lang(playerUuid), key);
    }

    public static void reloadLanguage(String language) {
        TranslationManager.getInstance().reloadLanguage(language);
    }

    public static void reloadAllLoaded() {
        TranslationManager.getInstance().reloadAllLoaded();
    }
}
