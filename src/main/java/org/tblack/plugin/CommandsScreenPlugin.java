package org.tblack.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class CommandsScreenPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public CommandsScreenPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void start() {
        // Called when the plugin starts
        getLogger().at(Level.INFO).log("MyFirstPlugin has started successfully!");

        // Registering the command (see Step 4)
        getCommandRegistry().registerCommand((new CommandsScreenCommand()));
    }

    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("Plugin stopping.");
    }
}