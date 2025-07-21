package com.rederealms.lobby.manager.item;

import com.rederealms.lobby.itens.Item;
import com.rederealms.lobby.manager.ItemManager.LobbyItemType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class ItemCache {

    private static final Map<LobbyItemType, ItemStack> cache = new EnumMap<>(LobbyItemType.class);

    static {
        cache.put(LobbyItemType.LOBBY_SERVERS, Item.LOBBY_SERVERS_ITEM());
        cache.put(LobbyItemType.LOBBY_HUB, Item.LOBBY_HUB_ITEM());
        cache.put(LobbyItemType.HIDE_PLAYERS, Item.LOBBY_HIDE_PLAYERS_ITEM());
        cache.put(LobbyItemType.SHOW_PLAYERS, Item.LOBBY_SHOW_PLAYERS_ITEM());
    }

    public static ItemStack getItem(LobbyItemType type, Player player) {
        if (type == LobbyItemType.LOBBY_PROFILE) {
            return Item.LOBBY_PROFILE_ITEM(player);
        }
        return cache.get(type).clone();
    }
}
