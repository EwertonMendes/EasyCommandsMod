package br.tblack.plugin;

import br.tblack.plugin.command.CommandsScreenCommand;
import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
import br.tblack.plugin.hud.HUDEvent;
import br.tblack.plugin.input.PacketListener;
import br.tblack.plugin.input.PlayerMovementStateSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class CommandsScreenPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private PacketFilter inboundFilter;

    public CommandsScreenPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("EasyCommands has started successfully!");

        getCommandRegistry().registerCommand(new CommandsScreenCommand());

        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, HUDEvent::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, HUDEvent::onPlayerDisconnect);

        ShortcutConfig.load();
        PlayerConfig.load();

        this.inboundFilter = PacketAdapters.registerInbound(new PacketListener());

        this.getEntityStoreRegistry().registerComponent(MovementStatesComponent.class, MovementStatesComponent::new);
        this.getEntityStoreRegistry().registerSystem(new PlayerMovementStateSystem());
    }

    @Override
    public void shutdown() {
        getLogger().at(Level.INFO).log("Plugin stopping.");
        if (this.inboundFilter == null) return;

        PacketAdapters.deregisterInbound(this.inboundFilter);
        this.inboundFilter = null;
    }
}