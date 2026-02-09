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
import com.hypixel.hytale.server.core.entity.entities.Player;
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

        Player player = store.getComponent(ref, Player.getComponentType());

        UUID uuid = player.getUuid();

        Map<String, String> commands = CommandUtils.getAllCommandsWithDescriptions();

        List<Map<String, Object>> commandsList = buildCommandsModel(commands);

        record Slot(int value){}

        List<Slot> slots = List.of(
                new Slot(1),
                new Slot(2),
                new Slot(3),
                new Slot(4),
                new Slot(5),
                new Slot(6),
                new Slot(7),
                new Slot(8),
                new Slot(9)
        );

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerName", playerRef.getUsername())
                .setVariable("slots", slots);

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef);

        pageBuilder
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        slots.forEach(slot -> {
            pageBuilder.getById("slot-" + slot.value + "-input", TextFieldBuilder.class).ifPresent(tx -> {
                tx.withValue(ShortcutConfig.getCommand(uuid.toString(), slot.value));
            });;

            pageBuilder.addEventListener("slot-" + slot.value + "-button", CustomUIEventBindingType.Activating, (_, ctx) -> {
                playerRef.sendMessage(Message.raw("Saving command on slot " + slot.value));
                String newCommandValue = ctx
                        .getValue("slot-" + slot.value + "-input")
                        .map(Object::toString)
                        .orElse("");

                try {
                    ShortcutConfig.setCommand(uuid.toString(), slot.value, newCommandValue);
                    playerRef.sendMessage(Message.raw("Command " + newCommandValue + " saved on slot " + slot.value + " successfully!").color(Color.GREEN));
                } catch (Exception e) {
                    playerRef.sendMessage(Message.raw("Error saving command " + newCommandValue + " on slot " + slot.value).color(Color.RED));
                }
            });
        });

        pageBuilder.open(store);
    }


    public static List<Map<String,Object>> buildCommandsModel(Map<String,String> commands) {
        List<Map<String,Object>> list = new ArrayList<>();
        for (Map.Entry<String,String> e : commands.entrySet()) {
            Map<String,Object> model = new HashMap<>();
            model.put("name", e.getKey());
            model.put("desc", e.getValue());
            list.add(model);
        }
        return list;
    }
}
