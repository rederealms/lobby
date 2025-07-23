package com.rederealms.lobby;

import com.rederealms.core.common.minecraft.misc.utils.api.ConfigAPI;
import com.rederealms.lobby.commands.*;
import com.rederealms.lobby.events.ChatListener;
import com.rederealms.lobby.events.LobbyListener;
import com.rederealms.lobby.services.*;
import com.rederealms.lobby.views.LobbySelectionView;
import com.rederealms.lobby.views.SelectServerView;
import lombok.Getter;
import me.devnatan.inventoryframework.ViewFrame;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class LobbyPlugin extends JavaPlugin {

    @Getter
    private static LobbyPlugin instance;
    private ConfigAPI location;
    private JedisPool jedisPool;
    private String serverName;
    private MultiServerChatService chatService;
    private ChatListener chatListener;
    private ChatControlService chatControlService;
    private ChatCommand chatCommand;
    private LobbyInfoService lobbyInfoService;
    private ViewFrame viewFrame;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        serverName = getConfig().getString("server-name", "lobby-1");
        initializeRedis();

        location = new ConfigAPI("location.yml", this);
        if (!location.exists()) location.saveDefaultConfig();

        chatListener = new ChatListener();

        if (jedisPool != null) {
            chatService = new MultiServerChatService(this, jedisPool, serverName);
            chatControlService = new ChatControlService(this, jedisPool, serverName);
            chatControlService.setChatListener(chatListener);
            lobbyInfoService = new LobbyInfoService(this, jedisPool, serverName);

            chatListener.setMultiServerChatService(chatService);
        }


        viewFrame = ViewFrame.create(this)
                .with(new LobbySelectionView(this),
                        new SelectServerView())
                        .register();

        BukkitFrame frame = new BukkitFrame(this);
        frame.registerCommands(
                new LocationCommand(this),
                new ChatCommand(this, jedisPool, serverName)
        );

        getServer().getPluginManager().registerEvents(new LobbyListener(this, viewFrame), this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    private void initializeRedis() {
        try {
            String host = getConfig().getString("redis.host", "localhost");
            int port = getConfig().getInt("redis.port", 6379);
            String password = getConfig().getString("redis.password", "");
            int database = getConfig().getInt("redis.database", 0);
            int timeout = getConfig().getInt("redis.timeout", 2000);

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(1);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(config, host, port, timeout, password, database);
            } else {
                jedisPool = new JedisPool(config, host, port, timeout, null, database);
            }

            getLogger().info("Conectado ao Redis para chat multi-servidor!");
        } catch (Exception e) {
            getLogger().warning("Falha ao conectar com Redis: " + e.getMessage());
            getLogger().warning("Chat multi-servidor desabilitado.");
        }
    }

    @Override
    public void onDisable() {
        if (chatService != null) {
            chatService.shutdown();
        }
        if (chatControlService != null) {
            chatControlService.shutdown();
        }
        if (lobbyInfoService != null) {
            lobbyInfoService.shutdown();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    public String getServerName() {
        return serverName;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public ViewFrame getViewFrame() {
        return viewFrame;
    }

    public LobbyInfoService getLobbyInfoService() {
        return lobbyInfoService;
    }

    public ConfigAPI getLocation() {
        return location;
    }
}