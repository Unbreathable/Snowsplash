package com.liphium.snowsplash.game.team.impl;

import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.util.LocationAPI;
import java.time.Duration;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ElfTeam extends Team {

	public ElfTeam() {
		super("Elves", "", Material.SPRUCE_SAPLING);
	}

	@Override
	public void giveKit(Player player, boolean teleport) {
		if (teleport) {
			player.teleport(
				Objects.requireNonNull(LocationAPI.getLocation("Elves"))
			);
		}
	}

	@Override
	public void sendStartMessage() {
		for (Player player : getPlayers()) {
			player.sendMessage(Component.text(" "));
			player.sendMessage(
				Component.text("    ", NamedTextColor.GRAY)
					.append(Component.text("You are an ", NamedTextColor.GRAY))
					.append(
						Component.text(
							"elf",
							NamedTextColor.AQUA,
							TextDecoration.BOLD
						)
					)
					.append(Component.text("!", NamedTextColor.GRAY))
			);
			player.sendMessage(Component.text(" "));
			player.sendMessage(
				Component.text("Deliver all ", NamedTextColor.GRAY)
					.append(Component.text("presents ", NamedTextColor.AQUA))
					.append(
						Component.text("into town and", NamedTextColor.GRAY)
					)
			);
			player.sendMessage(
				Component.text("make sure ", NamedTextColor.GRAY)
					.append(Component.text("everyone ", NamedTextColor.AQUA))
					.append(Component.text("gets one.", NamedTextColor.GRAY))
			);
			player.sendMessage(Component.text(" "));
		}
	}

	@Override
	public void handleWin() {
		Bukkit.broadcast(Component.text(" "));
		Bukkit.broadcast(
			Component.text("   ", NamedTextColor.AQUA)
				.append(Component.text("The ", NamedTextColor.AQUA))
				.append(
					Component.text(
						"Elves ",
						NamedTextColor.AQUA,
						TextDecoration.BOLD
					)
				)
				.append(Component.text("won the ", NamedTextColor.GRAY))
				.append(Component.text("game", NamedTextColor.AQUA))
				.append(Component.text("!", NamedTextColor.GRAY))
		);
		Bukkit.broadcast(Component.text(" "));
		Bukkit.broadcast(
			Component.text("All ", NamedTextColor.GRAY)
				.append(Component.text("presents ", NamedTextColor.AQUA))
				.append(
					Component.text(
						"were delivered and the ",
						NamedTextColor.GRAY
					)
				)
				.append(Component.text("everyone", NamedTextColor.AQUA))
		);
		Bukkit.broadcast(
			Component.text("can enjoy a beautiful ", NamedTextColor.GRAY)
				.append(Component.text("Christmas", NamedTextColor.AQUA))
				.append(Component.text("!", NamedTextColor.GRAY))
		);
		Bukkit.broadcast(Component.text(" "));

		for (Player player : getPlayers()) {
			player.showTitle(
				Title.title(
					Component.text(
						"Victory Royale",
						NamedTextColor.AQUA,
						TextDecoration.BOLD
					),
					Component.empty(),
					Title.Times.times(
						Duration.ofMillis(500),
						Duration.ofSeconds(3),
						Duration.ofMillis(500)
					)
				)
			);
			player.playSound(
				player.getLocation(),
				Sound.ITEM_GOAT_HORN_SOUND_1,
				1f,
				1f
			);
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!getPlayers().contains(player)) {
				player.showTitle(
					Title.title(
						Component.text(
							"Game Over",
							NamedTextColor.RED,
							TextDecoration.BOLD
						),
						Component.empty(),
						Title.Times.times(
							Duration.ofMillis(500),
							Duration.ofSeconds(3),
							Duration.ofMillis(500)
						)
					)
				);
				player.playSound(
					player.getLocation(),
					Sound.ITEM_GOAT_HORN_SOUND_1,
					1f,
					1f
				);
			}
		}
	}
}
