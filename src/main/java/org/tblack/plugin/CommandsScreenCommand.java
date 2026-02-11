package org.tblack.plugin;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CommandsScreenCommand extends AbstractPlayerCommand {

    public CommandsScreenCommand() {
        super("cmd", "Shows a screen with all available commands for the current player.");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {

        UUID uuid = context.sender().getUuid();

        List<Integer> slots = List.of(1,2,3,4,5,6,7,8,9);

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerName", playerRef.getUsername())
                .setVariable("slots", slots);

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef);

        pageBuilder
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        this.setSaveButtonEventListener(uuid, pageBuilder, playerRef, store, slots);
        this.setClearButtonsEventListener(uuid, pageBuilder, playerRef, store, slots);

        pageBuilder.open(store);
    }

    private void setClearButtonsEventListener(UUID uuid, PageBuilder pageBuilder, PlayerRef playerRef, Store<EntityStore> store, List<Integer> slots) {
        slots.forEach(slot -> {
           pageBuilder.getById("slot-" + slot + "-input", TextFieldBuilder.class).ifPresent(lb -> {
                lb.withValue(ShortcutConfig.getCommand(uuid.toString(), slot));
            });

            pageBuilder.addEventListener("slot-" + slot + "-button-clear", CustomUIEventBindingType.Activating, (_, ctx) -> {
                Map<Integer, String> currentValues = new HashMap<>();
                slots.forEach(slotIndex -> {
                    String v = ctx.getValue("slot-" + slotIndex + "-input").map(Object::toString).orElse("");
                    currentValues.put(slotIndex, v);
                });

                ShortcutConfig.removeCommand(uuid.toString(), slot);

                currentValues.put(slot, "");

                currentValues.forEach((slotIndex, value) -> {
                    ctx.getById("slot-" + slotIndex + "-input", TextFieldBuilder.class).ifPresent(tf -> tf.withValue(value));
                });

                ctx.updatePage(true);

                HUDEvent.refreshPlayerCommandsHud(playerRef, store);

                playerRef.sendMessage(
                        Message.raw("Removed command from slot " + slot + " successfully!").color(Color.orange)
                );
            });
        });
    }

    private void setSaveButtonEventListener(UUID uuid, PageBuilder pageBuilder, PlayerRef playerRef, Store<EntityStore> store, List<Integer> slots) {
        pageBuilder.addEventListener("save-all-button",  CustomUIEventBindingType.Activating, (_, ctx) -> {
            try {
                slots.forEach(slot -> {
                    String newCommandValue = ctx
                            .getValue("slot-" + slot + "-input")
                            .map(Object::toString)
                            .orElse("");

                    if (newCommandValue.isEmpty()) {
                        ShortcutConfig.removeCommand(uuid.toString(), slot);
                        return;
                    }
                    ShortcutConfig.setCommand(uuid.toString(), slot, newCommandValue);
                });
            } catch (Exception e){
                playerRef.sendMessage(Message.raw("Error saving commands, try again later!").color(Color.RED));
            }finally {
                HUDEvent.refreshPlayerCommandsHud(playerRef, store);
                playerRef.sendMessage(Message.raw("Commands saved successfully!").color(Color.GREEN));
            }
        });
    }
}
