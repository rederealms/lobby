package com.rederealms.lobby.views;

import com.rederealms.core.common.minecraft.CoreAPI;
import com.rederealms.core.common.minecraft.CoreSpigotPlugin;
import com.rederealms.core.common.minecraft.misc.utils.ItemBuilder;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class SelectServerView extends View {

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.title("Gerenciador de servidores");
        config.size(3);
        config.cancelOnPickup();
        config.cancelOnDrag();
        config.cancelOnDrop();
        config.cancelOnClick();
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        Player player = render.getPlayer();
        Component name = CoreSpigotPlugin.getCore().getComponent("messageMap.plugins.lobby.views.servers.rankup.name");
        List<Component> lore = CoreSpigotPlugin.getCore().getJsonList("messageMap.plugins.lobby.views.servers.rankup.lore");

        render.slot(13).withItem(new ItemBuilder(Material.BLAZE_POWDER)
                .name(name)
                .lore(lore)
                .build()).onClick(click ->{
           if(click.isLeftClick() || click.isRightClick() || click.isMiddleClick()) {
               CoreAPI.connect(player, "prison-spark");
           }
        });
    }
}
