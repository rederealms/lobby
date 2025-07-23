package com.rederealms.lobby.events;

import com.rederealms.core.common.minecraft.misc.adapters.LuckPermsAdapter;
import com.rederealms.core.common.minecraft.misc.lang.messages.Group;
import com.rederealms.lobby.services.MultiServerChatService;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ChatListener implements Listener {

    private final LuckPerms luckPerms;
    @Setter
    private static boolean chatEnabled = true;
    private final Set<Player> chatOffPlayers = new HashSet<>();
    @Setter
    private MultiServerChatService multiServerChatService;

    public ChatListener() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        this.luckPerms = provider != null ? provider.getProvider() : null;
    }

    @EventHandler
    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!chatEnabled && !player.hasPermission(Group.MODERADOR.getPermission())) {
            player.sendMessage(Component.text("§cO canal de chat está desabilitado no momento."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        
        String prefix = LuckPermsAdapter.getPrefix(player);
        String message = event.getMessage();
        String displayName = player.getName();

        TextComponent localChatFormat = LegacyComponentSerializer.legacyAmpersand()
            .deserialize(prefix + displayName + " §7» §f" + message);
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!chatOffPlayers.contains(online)) {
                online.sendMessage(localChatFormat);
            }
        }

        if (multiServerChatService != null) {
            multiServerChatService.sendChatMessage(player, message, prefix);
        }
    }
    public void togglePlayerChat(Player player) {
        if (chatOffPlayers.contains(player)) {
            chatOffPlayers.remove(player);
            player.sendMessage(Component.text("§aVocê ativou o chat."));
        } else {
            chatOffPlayers.add(player);
            player.sendMessage(Component.text("§cVocê desativou o chat."));
        }
    }

    public void clearChat() {
        Component emptyLine = Component.text("");

        for(int i = 0; i < 100; ++i) {
            Bukkit.getOnlinePlayers().forEach((p) -> {
                p.sendMessage(emptyLine);
            });
        }

        Bukkit.broadcast(Component.text("§aO chat foi limpo por um administrador."));
    }
    
    public static void setChatEnabled(boolean enabled) {
        chatEnabled = enabled;
    }
    
    public void setMultiServerChatService(MultiServerChatService service) {
        this.multiServerChatService = service;
    }
}

