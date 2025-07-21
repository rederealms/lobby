package com.rederealms.lobby.itens;

import com.rederealms.core.common.minecraft.CoreSpigotPlugin;
import com.rederealms.core.common.minecraft.misc.utils.ItemBuilder;
import com.rederealms.core.common.minecraft.misc.utils.SkullAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Item {
    public static ItemStack LOBBY_SERVERS_ITEM() {
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.items.lobby-servers-item.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.items.lobby-servers-item.lore");
        return new ItemBuilder(Material.COMPASS)
                .name(name)
                .lore(lore)
                .build();
    }

    public static ItemStack LOBBY_PROFILE_ITEM(Player player) {
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.items.lobby-profile-item.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.items.lobby-profile-item.lore");
        return new ItemBuilder(SkullAPI.getByName(player.getName()))
                .name(name)
                .lore(lore)
                .build();
    }

    public static ItemStack LOBBY_HUB_ITEM() {
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.items.lobby-hub-item.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.items.lobby-hub-item.lore");
        return new ItemBuilder(Material.NETHER_STAR)
                .name(name)
                .lore(lore)
                .build();
    }

    public static ItemStack LOBBY_HIDE_PLAYERS_ITEM() {
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.items.lobby-hide-item.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.items.lobby-hide-item.lore");

        return new ItemBuilder(Material.GRAY_DYE)
                .name(name)
                .lore(lore)
                .build();
    }

    public static ItemStack LOBBY_SHOW_PLAYERS_ITEM() {
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.items.lobby-show-item.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.items.lobby-show-item.lore");

        return new ItemBuilder(Material.LIME_DYE)
                .name(name)
                .lore(lore)
                .build();
    }
}

