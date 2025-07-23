package com.rederealms.lobby.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rederealms.lobby.LobbyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public class MultiServerChatService {
    private final LobbyPlugin plugin;
    private final JedisPool jedisPool;
    private final String serverName;
    private final String CHAT_CHANNEL = "global:lobby-chat";
    private JedisPubSub chatSubscriber;
    
    public MultiServerChatService(LobbyPlugin plugin, JedisPool jedisPool, String serverName) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverName = serverName;

        startChatListener();
    }
    
    public void sendChatMessage(Player player, String message, String prefix) {
        JsonObject chatData = new JsonObject();
        chatData.addProperty("server", serverName);
        chatData.addProperty("player", player.getName());
        chatData.addProperty("message", message);
        chatData.addProperty("prefix", prefix);
        chatData.addProperty("timestamp", System.currentTimeMillis());
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(CHAT_CHANNEL, chatData.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar mensagem para Redis: " + e.getMessage());
        }
    }
    
    private void startChatListener() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                chatSubscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (CHAT_CHANNEL.equals(channel)) {
                            handleIncomingChatMessage(message);
                        }
                    }
                };
                jedis.subscribe(chatSubscriber, CHAT_CHANNEL);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro no listener de chat Redis: " + e.getMessage());
            }
        });
    }
    
    public void shutdown() {
        if (chatSubscriber != null && chatSubscriber.isSubscribed()) {
            chatSubscriber.unsubscribe();
        }
    }
    
    private void handleIncomingChatMessage(String message) {
        try {
            JsonObject data = JsonParser.parseString(message).getAsJsonObject();
            String fromServer = data.get("server").getAsString();

            if (fromServer.equals(serverName)) {
                return;
            }
            
            String playerName = data.get("player").getAsString();
            String chatMessage = data.get("message").getAsString();
            String prefix = data.get("prefix").getAsString();
            
            // Formatar mensagem com indicador de servidor
            String formattedMessage = String.format("%s%s §7» §f%s",
                    prefix, playerName, chatMessage);
            
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage);

            // Enviar para todos os jogadores online no servidor atual
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(component);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar mensagem de chat: " + e.getMessage());
        }
    }
}