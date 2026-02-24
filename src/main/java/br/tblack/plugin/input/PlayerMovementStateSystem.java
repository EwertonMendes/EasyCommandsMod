package br.tblack.plugin.input;

import br.tblack.plugin.config.PlayerConfig;
import br.tblack.plugin.config.ShortcutConfig;
import br.tblack.plugin.hud.HUDEvent;
import br.tblack.plugin.hud.HudStore;
import br.tblack.plugin.i18n.Translations;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
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
        MovementStatesComponent movement = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        if (movement == null) return;

        var entityRef = chunk.getReferenceTo(index);

        Player player = store.getComponent(entityRef, Player.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        if (player == null || uuidComponent == null) return;

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            HUDEvent.onPlayerTick(playerRef, store);
        }

        UUID uuid = uuidComponent.getUuid();
        String uuidStr = uuid.toString();

        int highlightedSlot = resolveHighlightedSlot(player, uuidStr);
        updateHighlightedSlotIfChanged(uuid, highlightedSlot, entityRef, store);

        PlayerConfig.ActivationMode mode = PlayerConfig.getActivationMode(uuidStr);

        if (mode == PlayerConfig.ActivationMode.CTRL_F) {
            MovementStates states = movement.getMovementStates();
            handleCtrlF(player, commandBuffer, uuid, states);
            return;
        }

        if (mode == PlayerConfig.ActivationMode.O_ONLY) {
            handleOOnly(player, commandBuffer, uuid);
        }
    }

    private void handleOOnly(Player player, CommandBuffer<EntityStore> commandBuffer, UUID uuid) {

        if (!Boolean.TRUE.equals(InteractionTracker.pendingO.remove(uuid))) {
            return;
        }

        if (InteractionTracker.isShortcutSuppressed(uuid) || InteractionTracker.isOCooldown(uuid)) {
            return;
        }

        Boolean alreadyTriggered = InteractionTracker.triggeredShortcut.get(uuid);
        if (alreadyTriggered != null && alreadyTriggered) {
            return;
        }

        InteractionTracker.triggeredShortcut.put(uuid, true);
        InteractionTracker.startOCooldown(uuid);

        runSlot(player, commandBuffer, uuid);

        InteractionTracker.triggeredShortcut.remove(uuid);
    }

    private void handleCtrlF(Player player,
                             CommandBuffer<EntityStore> commandBuffer,
                             UUID uuid,
                             MovementStates states) {

        if (!states.crouching) {
            InteractionTracker.ctrlConsumed.remove(uuid);
            InteractionTracker.lastUsePress.remove(uuid);
            InteractionTracker.triggeredShortcut.remove(uuid);
            return;
        }

        if (!states.idle || !states.horizontalIdle) {
            InteractionTracker.triggeredShortcut.remove(uuid);
            return;
        }

        if (Boolean.TRUE.equals(InteractionTracker.ctrlConsumed.get(uuid))) {
            return;
        }

        Boolean alreadyTriggered = InteractionTracker.triggeredShortcut.get(uuid);
        if (alreadyTriggered != null && alreadyTriggered) return;

        Long lastUseTime = InteractionTracker.lastUsePress.get(uuid);
        if (lastUseTime == null) return;

        long now = System.currentTimeMillis();
        if (now - lastUseTime > InteractionTracker.USE_VALID_MS) return;

        InteractionTracker.triggeredShortcut.put(uuid, true);
        InteractionTracker.ctrlConsumed.put(uuid, true);

        runSlot(player, commandBuffer, uuid);

        InteractionTracker.lastUsePress.remove(uuid);
        InteractionTracker.triggeredShortcut.remove(uuid);
    }

    private void runSlot(Player player,
                         CommandBuffer<EntityStore> commandBuffer,
                         UUID uuid) {

        Inventory inv = player.getInventory();
        byte activeSlot = inv.getActiveHotbarSlot();
        int slotNumber = activeSlot + 1;

        String cmd = ShortcutConfig.getCommand(uuid.toString(), slotNumber);

        if (cmd != null && !cmd.isEmpty()) {
            player.sendMessage(
                    Translations.msgSuccess(uuid, "easycommands.msg.runningCommand", "cmd", cmd)
            );

            commandBuffer.run((_) ->
                    CommandManager.get().handleCommand(player, cmd)
            );
        } else {
            player.sendMessage(
                    Translations.msgWarning(uuid, "easycommands.msg.noCommandFoundForSlot", "slotNumber", slotNumber)
            );
        }
    }

    private static int resolveHighlightedSlot(Player player, String uuidStr) {
        int activeSlot = player.getInventory().getActiveHotbarSlot() + 1;

        String activeCommand = ShortcutConfig.getCommand(uuidStr, activeSlot);
        if (activeCommand == null || activeCommand.isEmpty()) return -1;

        return activeSlot;
    }

    private static void updateHighlightedSlotIfChanged(
            UUID uuid,
            int highlightedSlot,
            Ref entityRef,
            Store<EntityStore> store
    ) {
        int previous = HudStore.getHighlightedSlot(uuid);
        if (previous == highlightedSlot) return;

        HudStore.setHighlightedSlot(uuid, highlightedSlot);

        if (!HudStore.getIsVisible(uuid)) {
            HudStore.markDirty(uuid);
            return;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            HudStore.markDirty(uuid);
            return;
        }

        HUDEvent.onCommandsChanged(playerRef, store);
    }
}