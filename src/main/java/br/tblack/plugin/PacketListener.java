package br.tblack.plugin;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;

import java.util.UUID;

public class PacketListener implements PacketWatcher {
    @Override
    public void accept(PacketHandler packetHandler, Packet packet) {
        if (packet.getId() != 290) {
            return;
        }
        SyncInteractionChains interactionChains = (SyncInteractionChains) packet;
        SyncInteractionChain[] updates = interactionChains.updates;

        for (SyncInteractionChain item : updates) {
            PlayerAuthentication playerAuthentication = packetHandler.getAuth();
            assert playerAuthentication != null;
            UUID uuid = playerAuthentication.getUuid();

            InteractionType interactionType = item.interactionType;
            if (interactionType == InteractionType.Use) {
                InteractionTracker.lastUsePress.put(uuid, System.currentTimeMillis());
            }
        }
    }
}
