package org.tblack.plugin;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.Message;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static Map<String, String> getAllCommandsWithDescriptions() {

        CommandManager cm = CommandManager.get();
        if (cm == null) {
            throw new IllegalStateException("CommandManager not initialized");
        }

        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, AbstractCommand> entry : cm.getCommandRegistration().entrySet()) {

            String name = entry.getKey();
            AbstractCommand cmd = entry.getValue();

            String description;

            try {
                String descriptionKey = cmd.getDescription();
                description = Message.translation(descriptionKey).getAnsiMessage();
            } catch (Exception e) {
                description = "No description available";
            }

            result.put(name, description);
        }

        return result;
    }
}
