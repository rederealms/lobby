package com.rederealms.lobby.events;

import com.rederealms.core.common.minecraft.CoreSpigotPlugin;
import com.rederealms.core.common.minecraft.misc.adapters.LuckPermsAdapter;
import com.rederealms.core.common.minecraft.misc.lang.messages.Group;
import com.rederealms.core.common.minecraft.misc.utils.ItemBuilder;
import com.rederealms.lobby.LobbyPlugin;
import com.rederealms.lobby.manager.ItemManager;
import com.rederealms.lobby.manager.item.ItemCache;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class LobbyListener implements Listener {
    private final LobbyPlugin plugin;

    @EventHandler
     void onJoinPlayer(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String prefix = LuckPermsAdapter.getPrefix(player);
        player.teleport(plugin.getLocation().getLocation("lobby"));

        player.getInventory().setItem(0, ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_SERVERS, player));
        player.getInventory().setItem(3, ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_PROFILE, player));
        player.getInventory().setItem(5, ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_HUB, player));
        player.getInventory().setItem(8, ItemCache.getItem(ItemManager.LobbyItemType.SHOW_PLAYERS, player));

        if (prefix == null || prefix.isEmpty()) {
            return;
        }


        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);
            Component playerNameComponent = Component.text(player.getName());
            Component joinTextComponent = MiniMessage.miniMessage().deserialize("<gradient:#F1F87A:#FEF900:#FEDD00>entrou neste lobby!</gradient>");

            Component finalMessage = prefixComponent
                    .append(playerNameComponent)
                    .append(Component.space())
                    .append(joinTextComponent);

            onlinePlayer.sendMessage(finalMessage);
        }
    }

    @EventHandler
    void onInteractPlayer(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();

        if (itemInHand == null || itemInHand.getType().isAir()) return;

        int slot = player.getInventory().getHeldItemSlot();

        if (itemInHand.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_SERVERS, player))) {

            event.setCancelled(true);

        } else if (itemInHand.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_PROFILE, player))) {
            player.sendMessage("§eInventário de perfil ainda não feito.");
            event.setCancelled(true);

        } else if (itemInHand.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_HUB, player))) {
            player.sendMessage("§bVocê já está no hub!");
            event.setCancelled(true);

        } else if (itemInHand.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.HIDE_PLAYERS, player))) {
            togglePlayersVisibility(player, true);
            player.getInventory().setItem(slot, ItemCache.getItem(ItemManager.LobbyItemType.SHOW_PLAYERS, player));
            sendConfigMessage(player, "messageMap.plugins.lobby.items.lobby-hide-item.message");
            event.setCancelled(true);

        } else if (itemInHand.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.SHOW_PLAYERS, player))) {
            togglePlayersVisibility(player, false);
            player.getInventory().setItem(slot, ItemCache.getItem(ItemManager.LobbyItemType.HIDE_PLAYERS, player));
            sendConfigMessage(player, "messageMap.plugins.lobby.items.lobby-show-item.message");
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        if (
                clickedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_SERVERS, player)) ||
                        clickedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_PROFILE, player)) ||
                        clickedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_HUB, player)) ||
                        clickedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.HIDE_PLAYERS, player)) ||
                        clickedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.SHOW_PLAYERS, player))
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onUserDropItem(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (droppedItem == null || droppedItem.getType().isAir()) return;

        if (
                droppedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_SERVERS, player)) ||
                        droppedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_PROFILE, player)) ||
                        droppedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.LOBBY_HUB, player)) ||
                        droppedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.HIDE_PLAYERS, player)) ||
                        droppedItem.isSimilar(ItemCache.getItem(ItemManager.LobbyItemType.SHOW_PLAYERS, player))
        ) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    void onFallVoid(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getLocation().getY() <= 60.0) {
            player.teleport(plugin.getLocation().getLocation("lobby"));
            return;
        }
    }

    @EventHandler
    public void onDamageEvent(@NotNull BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(Group.MANAGER.getPermission())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(Group.MANAGER.getPermission())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void noFood(@NotNull FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void noPlayerDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void noWeatherChange(@NotNull WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void noMobs(@NotNull EntitySpawnEvent event) {
        event.setCancelled(true);
    }

    private void togglePlayersVisibility(Player player, boolean hide) {
        for(Player other : Bukkit.getOnlinePlayers()) {
            if(!other.equals(player)) {
                if(hide) {
                    player.hidePlayer(other);
                } else {
                    player.showPlayer(other);
                }
            }
        }
    }
    private void sendConfigMessage(Player player, String path) {
        Component message = CoreSpigotPlugin.getCore().getComponent(path);
        player.sendMessage(message);
    }
}

