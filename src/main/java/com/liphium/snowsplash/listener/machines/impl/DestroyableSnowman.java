package com.liphium.snowsplash.listener.machines.impl;

import com.liphium.snowsplash.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Snowman;

import java.util.Objects;

public class DestroyableSnowman extends Machine {

    private final Snowman man;

    public DestroyableSnowman(Location location, double health) {
        super(location, false);

        man = location.getWorld().spawn(location.clone(), Snowman.class);
        man.setAI(false);
        man.setGravity(false);

        // Set max health
        man.registerAttribute(Attribute.MAX_HEALTH);
        Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(health);
        man.setHealth(health);

        man.setCustomNameVisible(true);
        man.customName(getColoredHealthBar());
    }

    @Override
    public void tick() {
        man.setCustomNameVisible(true);
        man.customName(getColoredHealthBar());
        man.teleport(this.location);

        // TODO: Target players
    }

    public Component colorWithHealth(String text, NamedTextColor color, NamedTextColor background) {
        final double maxHealth = Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).getBaseValue();
        final double progress = man.getHealth() / maxHealth;

        // Color the string so it's green after a certain index
        final var splitPoint = (int) (text.length() * progress);
        return Component.text(text.substring(0, splitPoint), color)
                .append(Component.text(text.substring(splitPoint), background));
    }

    private Component getColoredHealthBar() {
        return colorWithHealth(getHealthBar(), NamedTextColor.GREEN, NamedTextColor.GRAY);
    }

    private String getHealthBar() {
        return String.format("■■■■■ %.0f/%.0f ■■■■■", man.getHealth(), Objects.requireNonNull(man.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
    }

    @Override
    public void destroy() {
        man.remove();
    }
}
