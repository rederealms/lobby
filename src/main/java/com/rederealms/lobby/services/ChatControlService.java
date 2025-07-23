package com.rederealms.lobby.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rederealms.lobby.LobbyPlugin;
import com.rederealms.lobby.events.ChatListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public class ChatControlService {
    private final LobbyPlugin plugin;
    private final JedisPool jedisPool;
    private final String serverName;
    private final String CHAT_CONTROL_CHANNEL = "global:chat-control";
    private ChatListener chatListener;
    private JedisPubSub controlSubscriber;
    
    public ChatControlService(LobbyPlugin plugin, JedisPool jedisPool, String serverName) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverName = serverName;
        
        startControlListener();
    }
    
    public void setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }
    
    private void startControlListener() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                controlSubscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (CHAT_CONTROL_CHANNEL.equals(channel)) {
                            handleControlMessage(message);
                        }
                    }
                };
                jedis.subscribe(controlSubscriber, CHAT_CONTROL_CHANNEL);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro no listener de controle de chat Redis: " + e.getMessage());
            }
        });
    }
    
    public void shutdown() {
        if (controlSubscriber != null && controlSubscriber.isSubscribed()) {
            controlSubscriber.unsubscribe();
        }
    }
    
    private void handleControlMessage(String message) {
        try {
            JsonObject data = JsonParser.parseString(message).getAsJsonObject();
            String fromServer = data.get("server").getAsString();

            if (fromServer.equals(serverName)) {
                return;
            }
            
            String action = data.get("action").getAsString();
            String adminName = data.get("admin").getAsString();
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (action) {
                    case "enable" -> {
                        ChatListener.setChatEnabled(true);
                        Bukkit.broadcast(Component.text("§aO chat foi ativado por " + adminName + " em " + fromServer + "."));
                    }
                    case "disable" -> {
                        ChatListener.setChatEnabled(false);
                        Bukkit.broadcast(Component.text("§cO chat foi desativado por " + adminName + " em " + fromServer + "."));
                    }
                    case "clear" -> {
                        if (chatListener != null) {
                            chatListener.clearChat();
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar comando de controle de chat: " + e.getMessage());
        }
    }
}