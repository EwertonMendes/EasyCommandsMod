package org.tblack.plugin;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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

        Map<String, String> commands = CommandUtils.getAllCommandsWithDescriptions();

        List<Map<String, Object>> commandsList = buildCommandsModel(commands);

        TemplateProcessor template = new TemplateProcessor()
                .setVariable("playerName", playerRef.getUsername())
                .setVariable("commands", commandsList);

        PageBuilder pageBuilder = PageBuilder.pageForPlayer(playerRef);

        pageBuilder
                .withLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction)
                .loadHtml("Pages/commands.html", template);

        commands.forEach((test, a) -> {
            pageBuilder.addEventListener(test.toString(), CustomUIEventBindingType.Activating, (data, ctx) -> {
                CommandManager.get().handleCommand(playerRef, test.toString());
                ctx.getPage().ifPresent(HyUIPage::close);

            });
        });
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
