package com.liphium.snowsplash.command;

import com.liphium.core.Core;
import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand implements CommandExecutor {

	@Override
	public boolean onCommand(
		CommandSender cs,
		Command cmd,
		String label,
		String[] args
	) {
		if (cs instanceof Player player && player.hasPermission("set")) {
			if (args.length == 0) {
				player.sendMessage(
					Component.text("/set <name> ", NamedTextColor.RED)
						.append(Component.text("-> ", NamedTextColor.DARK_GRAY))
						.append(
							Component.text(
								"Setzt eine Position.",
								NamedTextColor.GRAY
							)
						)
				);
			} else {
				if (args[0].equals("item")) {
					Core.getInstance().getScreens().open(player, 3);
					return true;
				}

				LocationAPI.setLocation(args[0], player.getLocation());
				player.sendMessage(
					Snowsplash.PREFIX.append(
						Component.text("Position ", NamedTextColor.RED).append(
							Component.text("gesetzt.", NamedTextColor.GRAY)
						)
					)
				);
			}
		} else {
			cs.sendMessage(Component.text("Keine Rechte!", NamedTextColor.RED));
		}

		return false;
	}
}
