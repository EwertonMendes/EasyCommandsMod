package org.tblack.plugin;

import au.ellie.hyui.builders.HudBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HUDEvent {

    public static void onPlayerReady(PlayerReadyEvent event) {

        Ref<EntityStore> playerRef = event.getPlayerRef();
        Store<EntityStore> store = playerRef.getStore();

        PlayerRef player = store.getComponent(playerRef, PlayerRef.getComponentType());

        HudBuilder.hudForPlayer(player)
                .loadHtml("Huds/quick-command-hud.html")
                .show(store);
    }

}
