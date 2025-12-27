package com.liphium.snowsplash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Snowsplash extends JavaPlugin {

    public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("Snowsplash", NamedTextColor.AQUA))
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
            .append(Component.text(" "));

    private static Snowsplash instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Snowsplash getInstance() {
        return instance;
    }
}
