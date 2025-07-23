package com.rederealms.lobby.views;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rederealms.lobby.LobbyPlugin;
import com.rederealms.lobby.services.LobbyInfoService;
import me.clip.placeholderapi.PlaceholderAPI;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class LobbySelectionView extends View {
    
    private final LobbyPlugin plugin;
    private final String currentServer;
    private final int currentLobbyNumber;
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();
    private long lastUpdate = 0;
    private long lastCacheVersion = -1;
    
    public LobbySelectionView(LobbyPlugin plugin) {
        this.plugin = plugin;
        this.currentServer = plugin.getServerName();
        this.currentLobbyNumber = extractLobbyNumber(currentServer);
    }
    
    private int extractLobbyNumber(String serverName) {
        String numberStr = serverName.replaceAll("\\D+", "");
        return numberStr.isEmpty() ? 1 : Integer.parseInt(numberStr);
    }
    
    @Override
    public void onInit(ViewConfigBuilder config) {
        config.title("ѕᴇʟᴇᴄɪᴏɴᴇ ᴏ ʟᴏʙʙʏ")
              .size(3)
              .cancelOnClick()
              .scheduleUpdate(100L);
    }

    @Override
    public void onFirstRender(RenderContext render) {
        if (plugin.getLobbyInfoService() != null) {
            lastCacheVersion = plugin.getLobbyInfoService().getCacheVersion();
        }
        renderAllItems(render);
    }
    
    @Override
    public void onUpdate(@NotNull Context update) {
        if (plugin.getLobbyInfoService() == null) {
            return;
        }
        
        long currentCacheVersion = plugin.getLobbyInfoService().getCacheVersion();
        long currentTime = System.currentTimeMillis();
        
        boolean cacheChanged = currentCacheVersion != lastCacheVersion;
        boolean timeExpired = currentTime - lastUpdate >= 3000;
        
        if (!cacheChanged && !timeExpired) {
            return;
        }
        
        lastUpdate = currentTime;
        lastCacheVersion = currentCacheVersion;
        
        if (cacheChanged) {
            itemCache.clear();
        }
        
        if (update instanceof RenderContext) {
            renderAllItems((RenderContext) update);
        }
    }
    
    private void renderAllItems(RenderContext render) {
        if (plugin.getLobbyInfoService() == null) {
            return;
        }
        
        Map<String, LobbyInfoService.LobbyInfo> onlineLobbies = plugin.getLobbyInfoService().getAvailableLobbies();
        
        TreeMap<Integer, String> allLobbies = new TreeMap<>();
        
        allLobbies.put(currentLobbyNumber, currentServer);
        
        for (LobbyInfoService.LobbyInfo lobby : onlineLobbies.values()) {
            int lobbyNumber = extractLobbyNumber(lobby.serverName);
            allLobbies.put(lobbyNumber, lobby.serverName);
        }
        
        int slot = 10;
        for (Map.Entry<Integer, String> entry : allLobbies.entrySet()) {
            if (slot > 16) break;
            
            String serverName = entry.getValue();
            boolean isCurrentServer = serverName.equals(currentServer);
            
            ItemStack item = createLobbyItem(serverName, isCurrentServer);
            
            if (isCurrentServer) {
                render.slot(slot, item);
            } else {
                render.slot(slot, item).onClick(click -> {
                    handleLobbyConnect(click, serverName);
                });
            }
            
            slot++;
        }
    }
    
    private ItemStack createLobbyItem(String serverName, boolean isCurrentServer) {
        Material material = isCurrentServer ? Material.GRAY_DYE : Material.LIME_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        int lobbyNumber = extractLobbyNumber(serverName);
        meta.setDisplayName("§aʟ #" + lobbyNumber);
        
        String playerCount = getPlayerCount(serverName);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(" §fᴊᴏɢᴀᴅᴏʀᴇѕ§7: " + playerCount);
        
        if (isCurrentServer) {
            lore.add("§eᴠᴏᴄê ᴇѕᴛá ᴄᴏɴᴇᴄᴛᴀᴅᴏ ᴀ ᴇѕᴛᴇ ʟᴏʙʙʏ.");
        } else {
            lore.add("§aᴄʟɪǫᴜᴇ ᴘᴀʀᴀ ѕᴇ ᴄᴏɴᴇᴄᴛᴀʀ.");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getPlayerCount(String serverName) {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Player dummyPlayer = Bukkit.getOnlinePlayers().iterator().hasNext() ? 
                    Bukkit.getOnlinePlayers().iterator().next() : null;
                
                if (dummyPlayer != null) {
                    String placeholder = "%bungee_" + serverName + "%";
                    String result = PlaceholderAPI.setPlaceholders(dummyPlayer, placeholder);
                    return result != null && !result.equals(placeholder) ? result : "0";
                }
            }
            
            if (serverName.equals(currentServer)) {
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            }
            
            return "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    private void handleLobbyConnect(SlotClickContext click, String targetServer) {
        Player player = click.getPlayer();
        
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            player.sendMessage("§aConectando ao " + targetServer + "...");
            
            click.closeForPlayer();
        } catch (Exception e) {
            player.sendMessage("§cErro ao conectar ao servidor!");
            plugin.getLogger().warning("Erro ao conectar jogador ao servidor: " + e.getMessage());
        }
    }
}