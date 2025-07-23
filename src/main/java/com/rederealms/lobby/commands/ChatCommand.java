package com.rederealms.lobby.commands;

import com.google.gson.JsonObject;
import com.rederealms.core.common.minecraft.misc.lang.messages.Group;
import com.rederealms.lobby.LobbyPlugin;
import com.rederealms.lobby.events.ChatListener;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ChatCommand {
    
    private final LobbyPlugin plugin;
    private final JedisPool jedisPool;
    private final String serverName;
    private final String CHAT_CONTROL_CHANNEL = "global:chat-control";
    private ChatListener chatListener;
    
    public ChatCommand(LobbyPlugin plugin, JedisPool jedisPool, String serverName) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverName = serverName;
    }
    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Command(name = "chat")
    public void handle(@NotNull Context<CommandSender> context, @Optional String action) {
        Player player = (Player) context.getSender();

        if (!player.hasPermission(Group.MANAGER.getPermission())) {
            Group.MANAGER.check(player);
            return;
        }

        if (action == null) {
            player.sendMessage("§cUso correto: /chat <on|off|viponly|clear>");
            return;
        }

        switch (action.toLowerCase()) {
            case "on" -> {
                ChatListener.setChatEnabled(true);
                sendChatControlMessage("enable", player.getName());
                Bukkit.broadcast(Component.text("§aO chat foi ativado por um administrador."));
            }
            case "off" -> {
                ChatListener.setChatEnabled(false);
                sendChatControlMessage("disable", player.getName());
                Bukkit.broadcast(Component.text("§cO chat foi desativado por um administrador."));
            }
            case "clear" -> {
                if (chatListener != null) {
                    chatListener.clearChat();
                } else {
                    Component emptyLine = Component.text("");
                    for(int i = 0; i < 100; i++) {
                        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(emptyLine));
                    }
                    Bukkit.broadcast(Component.text("§aO chat foi limpo por um administrador."));
                }
                sendChatControlMessage("clear", player.getName());
            }
            default -> player.sendMessage("§cUso correto: /chat <on|off|viponly|clear>");
        }
    }
    
    private void sendChatControlMessage(String action, String adminName) {
        if (jedisPool == null) {
            plugin.getLogger().warning("Redis não disponível para sincronização de chat!");
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject controlData = new JsonObject();
            controlData.addProperty("action", action);
            controlData.addProperty("server", serverName);
            controlData.addProperty("admin", adminName);
            controlData.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(CHAT_CONTROL_CHANNEL, controlData.toString());
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar comando de chat para Redis: " + e.getMessage());
        }
    }
}

