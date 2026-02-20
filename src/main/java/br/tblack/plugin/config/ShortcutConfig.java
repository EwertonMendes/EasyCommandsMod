package br.tblack.plugin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ShortcutConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR = "mods/EasyCommands";
    private static final String CONFIG_FILE = "shortcuts.json";

    private static final String LEGACY_CONFIG_DIR = "plugins";

    private static final Type DATA_TYPE = new TypeToken<Map<String, Map<Integer, String>>>() {}.getType();

    private static final JsonConfigStore<Map<String, Map<Integer, String>>> STORE =
            new JsonConfigStore<>(GSON, CONFIG_DIR, CONFIG_FILE, DATA_TYPE, HashMap::new);

    private static Map<String, Map<Integer, String>> shortcutsByPlayerUuid = new HashMap<>();

    public static void load() {
        migrateLegacyIfNeeded();
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

    private static void migrateLegacyIfNeeded() {
        migrateLegacyFileIfNeeded(LEGACY_CONFIG_DIR, CONFIG_FILE, CONFIG_DIR, CONFIG_FILE);
    }

    private static void migrateLegacyFileIfNeeded(String oldDir, String oldFile, String newDir, String newFile) {
        Path oldPath = Paths.get(oldDir, oldFile);
        Path newPath = Paths.get(newDir, newFile);

        try {
            if (!Files.exists(oldPath)) return;
            if (Files.exists(newPath)) return;

            Path newParent = newPath.getParent();
            if (newParent != null) Files.createDirectories(newParent);

            try {
                Files.move(oldPath, newPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(oldPath);
            }

            cleanupDirIfEmpty(Paths.get(oldDir));
        } catch (IOException e) {
            System.err.println("[EasyCommands] Failed to migrate legacy config: " + oldPath + " -> " + newPath);
            System.err.println("[EasyCommands] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void cleanupDirIfEmpty(Path dir) {
        try {
            if (!Files.isDirectory(dir)) return;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                if (stream.iterator().hasNext()) return;
            }

            Files.deleteIfExists(dir);

            Path parent = dir.getParent();
            if (parent != null) cleanupDirIfEmpty(parent);
        } catch (IOException ignored) {
        }
    }

    private static String normalizeCommandForStorage(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";

        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
        return trimmed;
    }
}