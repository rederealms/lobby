package com.rederealms.lobby;

import com.rederealms.core.common.minecraft.misc.utils.api.ConfigAPI;
import com.rederealms.lobby.commands.LocationCommand;
import com.rederealms.lobby.events.LobbyListener;
import lombok.Getter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import org.bukkit.plugin.java.JavaPlugin;

public class LobbyPlugin extends JavaPlugin {

    @Getter
    public LobbyPlugin instance;
    @Getter
    public ConfigAPI location;

    @Override
    public void onEnable() {
        instance = this;
        location = new ConfigAPI("location.yml", this);
        if (!location.exists()) location.saveDefaultConfig();

        BukkitFrame frame = new BukkitFrame(this);
        frame.registerCommands(
                new LocationCommand(this)
        );

        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
    }

    @Override
    public void onDisable() {
    }
}
