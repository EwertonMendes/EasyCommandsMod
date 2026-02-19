package br.tblack.plugin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ShortcutConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR = "plugins/EasyCommands";
    private static final String CONFIG_FILE = "shortcuts.json";

    private static final Type DATA_TYPE = new TypeToken<Map<String, Map<Integer, String>>>() {}.getType();

    private static final JsonConfigStore<Map<String, Map<Integer, String>>> STORE =
            new JsonConfigStore<>(GSON, CONFIG_DIR, CONFIG_FILE, DATA_TYPE, HashMap::new);

    private static Map<String, Map<Integer, String>> shortcutsByPlayerUuid = new HashMap<>();

    public static void load() {
        shortcutsByPlayerUuid = STORE.loadOrCreate();
        if (shortcutsByPlayerUuid == null) shortcutsByPlayerUuid = new HashMap<>();
    }

    public static void save() {
        STORE.save(shortcutsByPlayerUuid);
    }

    public static Map<Integer, String> getForPlayer(String uuid) {
        if (uuid == null || uuid.isBlank()) return new HashMap<>();
        return shortcutsByPlayerUuid.computeIfAbsent(uuid, ignored -> new HashMap<>());
    }

    public static String getCommand(String uuid, int slot) {
        return getForPlayer(uuid).getOrDefault(slot, "");
    }

    public static void setCommand(String uuid, int slot, String command) {
        if (uuid == null || uuid.isBlank() || command == null) return;

        String normalizedCommand = normalizeCommandForStorage(command);
        if (normalizedCommand.isEmpty()) {
            removeCommand(uuid, slot);
            return;
        }

        getForPlayer(uuid).put(slot, normalizedCommand);
        save();
    }

    public static void removeCommand(String uuid, int slot) {
        if (uuid == null || uuid.isBlank()) return;

        getForPlayer(uuid).remove(slot);
        save();
    }

    public static Map<String, Map<Integer, String>> getAll() {
        return shortcutsByPlayerUuid;
    }

    private static String normalizeCommandForStorage(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";

        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
        return trimmed;
    }
}
