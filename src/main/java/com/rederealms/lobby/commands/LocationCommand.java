package com.rederealms.lobby.commands;

import com.rederealms.core.common.minecraft.CoreSpigotPlugin;
import com.rederealms.core.common.minecraft.misc.lang.messages.Group;
import com.rederealms.core.common.shared.misc.eventbus.Logger;
import com.rederealms.lobby.LobbyPlugin;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.command.Context;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LocationCommand {
    private final LobbyPlugin plugin;

    public LocationCommand(LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(name = "location")

    public void handle(@NotNull Context<CommandSender> context) {
        Player player = (Player) context.getSender();
        if(!player.hasPermission(Group.MANAGER.getPermission())) {
            Group.MANAGER.check(player);
            return;
        }

        plugin.getLocation().setLocation("lobby", player.getLocation());
        player.sendMessage(CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.command.location.set"));
        plugin.getLocation().saveConfig();

    }
}
