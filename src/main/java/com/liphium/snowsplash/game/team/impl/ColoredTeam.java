package com.liphium.snowsplash.game.team.impl;

import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

public class ColoredTeam extends Team {

    public ColoredTeam(String name, NamedTextColor color, Material material) {
        super(name, color, material);
    }

    @Override
    public void giveKit(Player player, boolean teleport) {
        if (teleport) {
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation(this.getName())));
        }
    }

    @Override
    public void sendStartMessage() {
        for (Player player : getPlayers()) {
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("    ", NamedTextColor.GRAY)
                    .append(Component.text("You are in team ", NamedTextColor.GRAY))
                    .append(Component.text(this.getName(), this.getColor(), TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY)));
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("Destroy the ", NamedTextColor.GRAY)
                    .append(Component.text("snowman ", NamedTextColor.AQUA))
                    .append(Component.text("on the other", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("side to win the game!", NamedTextColor.GRAY));
            player.sendMessage(Component.text(" "));
        }
    }

    @Override
    public void handleWin() {
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("   ", NamedTextColor.GRAY)
                .append(Component.text("Team ", this.getColor()))
                .append(Component.text(this.getName(), this.getColor(), TextDecoration.BOLD))
                .append(Component.text("won the ", NamedTextColor.GRAY))
                .append(Component.text("game", this.getColor()))
                .append(Component.text("!", NamedTextColor.GRAY)));
        Bukkit.broadcast(Component.text(" "));

        for (Player player : getPlayers()) {
            player.showTitle(Title.title(Component.text("Victory Royale", NamedTextColor.GREEN, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getPlayers().contains(player)) {
                player.showTitle(Title.title(Component.text("Game Lost", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
                player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }
    }
}
