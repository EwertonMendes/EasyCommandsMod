package org.tblack.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
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
    private PacketListener packetListener;

    public CommandsScreenPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("EasyCommands has started successfully!");

        getCommandRegistry().registerCommand((new CommandsScreenCommand()));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HUDEvent::onPlayerReady);

        this.packetListener = new PacketListener();
        PacketAdapters.registerInbound(this.packetListener);

        ShortcutConfig.load();

        this.getEntityStoreRegistry().registerComponent(MovementStatesComponent.class, MovementStatesComponent::new);
        this.getEntityStoreRegistry().registerSystem(new PlayerMovementStateSystem());
    }

    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("Plugin stopping.");
        if (this.packetListener != null) {
            PacketAdapters.registerInbound(this.packetListener);
        }
    }
}

