package org.tblack.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ShortcutConfig {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "plugins/Shortcuts";
    private static final String CONFIG_FILE = "shortcuts.json";

    private static Map<String, Map<Integer, String>> perPlayerShortcuts = new HashMap<>();

    public static void load() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, CONFIG_FILE);
            if (!file.exists()) {
                save();
                return;
            }

            Type type = new TypeToken<Map<String, Map<Integer, String>>>() {}.getType();
            Map<String, Map<Integer, String>> data =
                    gson.fromJson(new FileReader(file), type);

            if (data != null) {
                perPlayerShortcuts = data;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, CONFIG_FILE);
            FileWriter writer = new FileWriter(file);
            gson.toJson(perPlayerShortcuts, writer);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, String> getForPlayer(String uuid) {
        return perPlayerShortcuts.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static String getCommand(String uuid, int slot) {
        return getForPlayer(uuid).getOrDefault(slot, "");
    }

    public static void setCommand(String uuid, int slot, String command) {

        if (command == null) return;

        command = command.trim();

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (command.isEmpty()) {
            removeCommand(uuid, slot);
            return;
        }

        Map<Integer, String> map = getForPlayer(uuid);
        map.put(slot, command);
        save();
    }

    public static void removeCommand(String uuid, int slot) {
        Map<Integer, String> map = getForPlayer(uuid);
        map.remove(slot);
        save();
    }


    public static Map<String, Map<Integer, String>> getAll() {
        return perPlayerShortcuts;
    }
}
