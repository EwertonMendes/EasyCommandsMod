package br.tblack.plugin;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.CommandManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.UUID;

public class PlayerMovementStateSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                Player.getComponentType(),
                MovementStatesComponent.getComponentType()
        );
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {

        MovementStatesComponent movement =
                chunk.getComponent(index, MovementStatesComponent.getComponentType());

        if (movement == null) return;

        MovementStates states = movement.getMovementStates();

        var playerRef = chunk.getReferenceTo(index);

        Player player = store.getComponent(playerRef, Player.getComponentType());
        UUIDComponent UuidComponent = store.getComponent(playerRef, UUIDComponent.getComponentType());

        assert UuidComponent != null;
        UUID uuid = UuidComponent.getUuid();

        if (!states.crouching || !states.idle || !states.horizontalIdle) {
            InteractionTracker.triggeredShortcut.remove(uuid);
            return;
        }

        Boolean alreadyTriggered = InteractionTracker.triggeredShortcut.get(uuid);
        if (alreadyTriggered != null && alreadyTriggered) {
            return;
        }

        Long lastUseTime = InteractionTracker.lastUsePress.get(uuid);
        if (lastUseTime == null) return;

        long now = System.currentTimeMillis();

        if (now - lastUseTime <= InteractionTracker.USE_VALID_MS) {

            InteractionTracker.triggeredShortcut.put(uuid, true);

            Inventory inv = player.getInventory();
            byte activeSlot = inv.getActiveHotbarSlot();
            int slotNumber = activeSlot + 1;

            String cmd = ShortcutConfig.getCommand(uuid.toString(), slotNumber);

            if (cmd != null && !cmd.isEmpty()) {

                player.sendMessage(Message.raw("Running Command /" + cmd).color(Color.GREEN));

                commandBuffer.run((_) ->
                        CommandManager.get().handleCommand(player, cmd)
                );
            } else {
                player.sendMessage(Message.raw("No configured command for slot " + slotNumber));
            }

            InteractionTracker.lastUsePress.remove(uuid);
        }
    }
}
