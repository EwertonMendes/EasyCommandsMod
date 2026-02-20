package br.tblack.plugin.i18n;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.LocalizableString;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TranslationManager {

    private static final String LANG_FILE_PATH = "Server/Languages/%s/easycommands.lang";
    private static final String DEFAULT_LANGUAGE = "en-US";
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static TranslationManager instance;

    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    private TranslationManager() {
        loadLanguage(DEFAULT_LANGUAGE);
    }

    public static TranslationManager getInstance() {
        if (instance == null) {
            synchronized (TranslationManager.class) {
                if (instance == null) instance = new TranslationManager();
            }
        }
        return instance;
    }

    public void loadLanguage(String language) {
        if (language == null || language.isBlank()) language = DEFAULT_LANGUAGE;
        String path = LANG_FILE_PATH.formatted(language);

        Map<String, String> langMap = new HashMap<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                if (!DEFAULT_LANGUAGE.equals(language)) return;
                translations.put(DEFAULT_LANGUAGE, langMap);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex == -1) continue;

                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    langMap.put(key, value);
                }
            }

            translations.put(language, langMap);
        } catch (Exception ignored) {
        }
    }

    public void ensureLanguageLoaded(String language) {
        if (language == null || language.isBlank()) language = DEFAULT_LANGUAGE;
        if (!translations.containsKey(language)) loadLanguage(language);
    }

    public String getRaw(String language, String key) {
        if (language == null || language.isBlank()) language = DEFAULT_LANGUAGE;
        ensureLanguageLoaded(language);

        Map<String, String> currentLang = translations.get(language);
        if (currentLang != null) {
            String value = currentLang.get(key);
            if (value != null) return value;
        }

        if (!DEFAULT_LANGUAGE.equals(language)) {
            Map<String, String> fallback = translations.get(DEFAULT_LANGUAGE);
            if (fallback != null) {
                String value = fallback.get(key);
                if (value != null) return value;
            }
        }

        return key;
    }

    public String get(String language, String key, Object... params) {
        String raw = getRaw(language, key);
        if (params == null || params.length == 0) return raw;

        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i + 1 < params.length; i += 2) {
            paramMap.put(String.valueOf(params[i]), params[i + 1]);
        }

        return format(raw, paramMap);
    }

    public String get(String language, String key, Map<String, Object> params) {
        String raw = getRaw(language, key);
        return format(raw, params);
    }

    private String format(String text, Map<String, Object> params) {
        if (params == null || params.isEmpty()) return text;

        Matcher matcher = PARAM_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.get(paramName);
            String replacement = value != null ? String.valueOf(value) : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public Message message(String language, String key, Object... params) {
        return Message.raw(get(language, key, params));
    }

    public Message message(String language, String key, Color color, Object... params) {
        return Message.raw(get(language, key, params)).color(color);
    }

    public LocalizableString localizable(String language, String key, Object... params) {
        return LocalizableString.fromString(get(language, key, params));
    }

    public Set<String> getLoadedLanguages() {
        return translations.keySet();
    }

    public boolean hasKey(String language, String key) {
        if (language == null || language.isBlank()) language = DEFAULT_LANGUAGE;
        ensureLanguageLoaded(language);

        Map<String, String> current = translations.get(language);
        if (current != null && current.containsKey(key)) return true;

        Map<String, String> fallback = translations.get(DEFAULT_LANGUAGE);
        return fallback != null && fallback.containsKey(key);
    }

    public void reloadLanguage(String language) {
        if (language == null || language.isBlank()) language = DEFAULT_LANGUAGE;
        translations.remove(language);
        loadLanguage(language);
    }

    public void reloadAllLoaded() {
        Iterator<String> it = new ArrayList<>(translations.keySet()).iterator();
        while (it.hasNext()) {
            String lang = it.next();
            reloadLanguage(lang);
        }
    }
}
