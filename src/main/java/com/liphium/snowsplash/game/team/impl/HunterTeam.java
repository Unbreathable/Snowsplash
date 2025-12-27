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

public class HunterTeam extends Team {

    public HunterTeam() {
        super("Hunters", "", Material.IRON_SWORD);
    }

    @Override
    public void giveKit(Player player, boolean teleport) {
        if (teleport) {
            player.teleport(Objects.requireNonNull(LocationAPI.getLocation("Hunters")));
        }
    }

    @Override
    public void sendStartMessage() {
        for (Player player : getPlayers()) {
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("    ", NamedTextColor.GRAY).append(Component.text("You are a ", NamedTextColor.GRAY)).append(Component.text("hunter", NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.text("!", NamedTextColor.GRAY)));
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("Prevent the ", NamedTextColor.GRAY).append(Component.text("elves ", NamedTextColor.AQUA)).append(Component.text("from giving out", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("presents ", NamedTextColor.AQUA).append(Component.text("and be happy about it.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text(" "));
        }
    }

    @Override
    public void handleWin() {
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("   ", NamedTextColor.RED).append(Component.text("The ", NamedTextColor.RED)).append(Component.text("Hunters ", NamedTextColor.RED, TextDecoration.BOLD)).append(Component.text("won the ", NamedTextColor.GRAY)).append(Component.text("game", NamedTextColor.RED)).append(Component.text("!", NamedTextColor.GRAY)));
        Bukkit.broadcast(Component.text(" "));
        Bukkit.broadcast(Component.text("The ", NamedTextColor.GRAY).append(Component.text("elves ", NamedTextColor.RED)).append(Component.text("weren't able to hand out all", NamedTextColor.GRAY)));
        Bukkit.broadcast(Component.text("presents ", NamedTextColor.RED).append(Component.text("in time. What a shame!", NamedTextColor.GRAY)));
        Bukkit.broadcast(Component.text(" "));

        for (Player player : getPlayers()) {
            player.showTitle(Title.title(Component.text("Victory Royale", NamedTextColor.AQUA, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))));
            player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getPlayers().contains(player)) {
                player.showTitle(Title.title(Component.text("Game Over", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))));
                player.playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1f);
            }
        }
    }
}
