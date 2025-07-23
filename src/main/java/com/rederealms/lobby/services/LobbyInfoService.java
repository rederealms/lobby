package com.rederealms.lobby.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rederealms.core.common.minecraft.CoreAPI;
import com.rederealms.lobby.LobbyPlugin;
import com.rederealms.lobby.views.LobbySelectionView;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyInfoService {
    private final LobbyPlugin plugin;
    private final JedisPool jedisPool;
    private final String serverName;
    private final String LOBBY_INFO_CHANNEL = "global:lobby-info";
    private final String LOBBY_REQUEST_CHANNEL = "global:lobby-request";
    private final String LOBBY_STATUS_CHANNEL = "global:lobby-status";
    private final Map<String, LobbyInfo> lobbyServers = new ConcurrentHashMap<>();
    private JedisPubSub infoSubscriber;
    private volatile long cacheVersion = 0;
    
    public LobbyInfoService(LobbyPlugin plugin, JedisPool jedisPool, String serverName) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverName = serverName;
        
        startInfoListener();
        startHeartbeat();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendServerStartup();
            requestOnlineServers();
        }, 20L);
    }
    
    private void startInfoListener() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = jedisPool.getResource()) {
                infoSubscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (LOBBY_INFO_CHANNEL.equals(channel)) {
                            handleLobbyInfo(message);
                        } else if (LOBBY_REQUEST_CHANNEL.equals(channel)) {
                            handleLobbyRequest(message);
                        } else if (LOBBY_STATUS_CHANNEL.equals(channel)) {
                            handleLobbyStatus(message);
                        }
                    }
                };
                jedis.subscribe(infoSubscriber, LOBBY_INFO_CHANNEL, LOBBY_REQUEST_CHANNEL, LOBBY_STATUS_CHANNEL);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro no listener de informações de lobby: " + e.getMessage());
            }
        });
    }
    
    private void startHeartbeat() {
        // Envia heartbeat a cada 10 segundos
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendHeartbeat, 100L, 200L);
        // Limpa servidores antigos a cada 10 segundos
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupOldServers, 20L, 200L);
        // Solicita lista de servidores a cada 30 segundos
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::requestLobbyList, 200L, 600L);
    }
    
    private void cleanupOldServers() {
        int sizeBefore = lobbyServers.size();
        lobbyServers.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue().timestamp > 30000);
        
        if (lobbyServers.size() != sizeBefore) {
            invalidateCache();
        }
    }
    
    private void sendHeartbeat() {
        if (jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("server", serverName);
            heartbeat.addProperty("players", Bukkit.getOnlinePlayers().size());
            heartbeat.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(LOBBY_INFO_CHANNEL, heartbeat.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar heartbeat: " + e.getMessage());
        }
    }
     //aq ele pede a lista de sv
    public void requestLobbyList() {
        if (jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject request = new JsonObject();
            request.addProperty("requester", serverName);
            request.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(LOBBY_REQUEST_CHANNEL, request.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao solicitar lista de lobbys: " + e.getMessage());
        }
    }
    
    private void handleLobbyInfo(String message) {
        if (message == null || message.isEmpty()) return;
        
        try {
            JsonObject data = JsonParser.parseString(message).getAsJsonObject();
            
            if (!data.has("server") || !data.has("players") || !data.has("timestamp")) {
                return;
            }
            
            String server = data.get("server").getAsString();
            if (server.equals(serverName)) return;
            
            int players = data.get("players").getAsInt();
            long timestamp = data.get("timestamp").getAsLong();
            
            boolean isNewServer = !lobbyServers.containsKey(server);
            lobbyServers.put(server, new LobbyInfo(server, players, timestamp));
            
            if (isNewServer) {
                invalidateCache();
            }
                
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar informação de lobby: " + e.getMessage());
        }
    }
    
    private void handleLobbyRequest(String message) {
        if (message == null || message.isEmpty()) return;
        
        try {
            JsonObject data = JsonParser.parseString(message).getAsJsonObject();
            
            if (!data.has("requester")) return;
            
            String requester = data.get("requester").getAsString();
            if (!requester.equals(serverName)) {
                Bukkit.getScheduler().runTaskLater(plugin, this::sendHeartbeat, 5L);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar solicitação de lobby: " + e.getMessage());
        }
    }
    
    public Map<String, LobbyInfo> getAvailableLobbies() {
        return new ConcurrentHashMap<>(lobbyServers);
    }
    
    public void shutdown() {
        sendServerShutdown();
        if (infoSubscriber != null && infoSubscriber.isSubscribed()) {
            infoSubscriber.unsubscribe();
        }
    }
    
    private void sendServerStartup() {
        if (jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject startup = new JsonObject();
            startup.addProperty("action", "add");
            startup.addProperty("server", serverName);
            startup.addProperty("players", Bukkit.getOnlinePlayers().size());
            startup.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(LOBBY_STATUS_CHANNEL, startup.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar sinal de startup: " + e.getMessage());
        }
    }
    
    private void sendServerShutdown() {
        if (jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject shutdown = new JsonObject();
            shutdown.addProperty("action", "remove");
            shutdown.addProperty("server", serverName);
            shutdown.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(LOBBY_STATUS_CHANNEL, shutdown.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao enviar sinal de shutdown: " + e.getMessage());
        }
    }
    

    
    private void requestOnlineServers() {
        if (jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "get");
            request.addProperty("requester", serverName);
            request.addProperty("timestamp", System.currentTimeMillis());
            
            jedis.publish(LOBBY_STATUS_CHANNEL, request.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao solicitar servidores online: " + e.getMessage());
        }
    }
    
    private void handleLobbyStatus(String message) {
        if (message == null || message.isEmpty()) return;
        
        try {
            JsonObject data = JsonParser.parseString(message).getAsJsonObject();
            
            if (!data.has("action")) return;
            
            String action = data.get("action").getAsString();
            String server = "";
            
            if ("get".equals(action)) {
                if (!data.has("requester")) return;
                server = data.get("requester").getAsString();
            } else {
                if (!data.has("server")) return;
                server = data.get("server").getAsString();
            }
            
            if (server.equals(serverName)) return;
            
            switch (action) {
                case "add" -> {
                    int players = data.has("players") ? data.get("players").getAsInt() : 0;
                    long timestamp = data.get("timestamp").getAsLong();
                    lobbyServers.put(server, new LobbyInfo(server, players, timestamp));
                    invalidateCache();
                }
                case "remove" -> {
                    lobbyServers.remove(server);
                    invalidateCache();
                }
                case "get" -> {
                    sendHeartbeat();
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar status de lobby: " + e.getMessage());
        }
    }

    
    private void invalidateCache() {
        cacheVersion++;
    }
    
    public long getCacheVersion() {
        return cacheVersion;
    }
    
    public void forceRefresh() {
        requestLobbyList();
        sendHeartbeat();
    }
    
    public static class LobbyInfo {
        public final String serverName;
        public final int playerCount;
        public final long timestamp;
        
        public LobbyInfo(String serverName, int playerCount, long timestamp) {
            this.serverName = serverName;
            this.playerCount = playerCount;
            this.timestamp = timestamp;
        }
    }
}