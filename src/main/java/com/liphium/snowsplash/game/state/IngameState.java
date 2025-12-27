package com.liphium.snowsplash.game.state;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.GameState;
import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.game.team.impl.ElfTeam;
import com.liphium.snowsplash.game.team.impl.HunterTeam;
import com.liphium.snowsplash.listener.machines.impl.PresentReceiver;
import com.liphium.snowsplash.util.LocationAPI;
import com.liphium.snowsplash.util.Messages;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class IngameState extends GameState {

	private final ArrayList<DroppableTrap> traps = new ArrayList<>();

	private Runnable runnable;

	public IngameState() {
		super("In game", 30);
	}

	private int presentsLeft = 0;
	private final HashMap<String, PresentReceiver> receivers = new HashMap<>();
	private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();
	private final HashMap<Player, String> currentDelivery = new HashMap<>();

	@Override
	public void start() {
		// World cleanup
		for (Entity entity : Objects.requireNonNull(
			Bukkit.getWorld("elfhunt")
		).getEntities()) {
			if (
				entity.getType() != EntityType.ARMOR_STAND &&
				entity.getType() != EntityType.PLAYER
			) entity.remove();
		}

		// Change the amount of presents based on team size
		final var hunterSize = Snowsplash.getInstance()
			.getGameManager()
			.getTeamManager()
			.getTeam("Elves")
			.getPlayers()
			.size();
		int maxPresents = hunterSize * 4; // 4 per member of the team seems fine for 15 minutes
		presentsLeft = maxPresents;

		// Give every single present receiver a random name
		for (PresentReceiver receiver : Snowsplash.getInstance()
			.getMachineManager()
			.getMachines(PresentReceiver.class)) {
			var name = PresentReceiver.randomName();
			while (receivers.containsKey(name)) {
				name = PresentReceiver.randomName();
			}
			receiver.assignName(name);
			receivers.put(name, receiver);
		}

		for (Team team : Snowsplash.getInstance()
			.getGameManager()
			.getTeamManager()
			.getTeams()) {
			team.sendStartMessage();

			for (Player player : team.getPlayers()) {
				player.getInventory().clear();
				player.setHealth(20);
				team.giveKit(player, true);
			}
		}

		Snowsplash.getInstance()
			.getTaskManager()
			.inject(
				runnable = new Runnable() {
					int tickCount = 0;
					int countdown = 20 * 60 * 15; // 15 minutes in ticks

					@Override
					public void run() {
						Snowsplash.getInstance()
							.getGameManager()
							.getTeamManager()
							.tick();
						Snowsplash.getInstance().getMachineManager().tick();

						if (tickCount++ >= 20) {
							tickCount = 0;

							// Check if the win condition for the hunters is met
							if (countdown <= 0) {
								handleWin(
									Snowsplash.getInstance()
										.getGameManager()
										.getTeamManager()
										.getTeam("Hunters")
								);
								return;
							}

							Messages.actionBar(
                                    Component.text(presentsLeft, NamedTextColor.AQUA)
									.append(Component.text("/", NamedTextColor.GRAY))
									.append(Component.text(maxPresents, NamedTextColor.AQUA))
									.appendSpace()
									.append(Component.text("remaining", NamedTextColor.GRAY))
									.appendSpace()
									.append(Component.text("|", NamedTextColor.DARK_GRAY))
									.appendSpace()
									.append(formatTicks(countdown))
                                    .appendSpace()
                                    .append(Component.text("left",NamedTextColor.GRAY))
							);
						}

						countdown--;
					}
				}
			);
	}

	private Component formatTicks(int ticks) {
		int totalSeconds = ticks / 20; // 20 ticks = 1 second
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;

		return Component.text(
			String.format("%02d", minutes),
			NamedTextColor.AQUA
		)
			.append(Component.text(":", NamedTextColor.GRAY))
			.append(
				Component.text(
					String.format("%02d", seconds),
					NamedTextColor.AQUA
				)
			);
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (
			event.getItem() != null &&
			event.getItem().getType() == Material.WIND_CHARGE
		) {
			return;
		}
		Snowsplash.getInstance().getMachineManager().onInteract(event);

		if (event.getItem() != null) {
			Team team = Snowsplash.getInstance()
				.getGameManager()
				.getTeamManager()
				.getTeam(event.getPlayer());
			ItemStack usedItem = event.getItem();
			if (
				usedItem.getType().equals(Material.GRAY_DYE) &&
				event.getClickedBlock() != null
			) {
				traps.add(
					new SlowTrap(
						event
							.getClickedBlock()
							.getLocation()
							.clone()
							.add(0.5, 1, 0.5),
						team
					)
				);
				reduceMainHandItem(event.getPlayer(), Material.GRAY_DYE);
			} else if (
				usedItem.getType().equals(Material.LIME_DYE) &&
				event.getClickedBlock() != null
			) {
				traps.add(
					new PoisonTrap(
						event
							.getClickedBlock()
							.getLocation()
							.clone()
							.add(0.5, 1, 0.5),
						team
					)
				);
				reduceMainHandItem(event.getPlayer(), Material.LIME_DYE);
			} else if (
				usedItem.getType().equals(Material.FEATHER) &&
				event.getClickedBlock() != null
			) {
				traps.add(
					new FlyTrap(
						event
							.getClickedBlock()
							.getLocation()
							.clone()
							.add(0.5, 1, 0.5),
						team
					)
				);
				reduceMainHandItem(event.getPlayer(), Material.FEATHER);
			} else if (
				usedItem.getType().equals(Material.LIGHT_BLUE_DYE) &&
				event.getClickedBlock() != null
			) {
				traps.add(
					new FreezeTrap(
						event
							.getClickedBlock()
							.getLocation()
							.clone()
							.add(0.5, 1, 0.5),
						team
					)
				);
				reduceMainHandItem(event.getPlayer(), Material.LIGHT_BLUE_DYE);
			} else if (
				usedItem.getType().equals(Material.WHITE_DYE) &&
				event.getClickedBlock() != null
			) {
				traps.add(
					new WebTrap(
						event
							.getClickedBlock()
							.getLocation()
							.clone()
							.add(0.5, 1, 0.5),
						team
					)
				);
				reduceMainHandItem(event.getPlayer(), Material.WHITE_DYE);
			}
		}
	}

	void reduceMainHandItem(Player player, Material material) {
		if (player.getInventory().getItemInMainHand().getType() == material) {
			int amount = player.getInventory().getItemInMainHand().getAmount();
			if (amount == 1) {
				player.getInventory().setItemInMainHand(null);
			} else player
				.getInventory()
				.getItemInMainHand()
				.setAmount(amount - 1);
		} else if (
			player.getInventory().getItemInOffHand().getType() == material
		) {
			int amount = player.getInventory().getItemInOffHand().getAmount();
			if (amount == 1) {
				player.getInventory().setItemInOffHand(null);
			} else player
				.getInventory()
				.getItemInOffHand()
				.setAmount(amount - 1);
		}
	}

	@Override
	public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
		Snowsplash.getInstance().getMachineManager().onInteractAtEntity(event);

		if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
			event.setCancelled(true);
		}
	}

	private Component buildMessage(String template, String playerName) {
		String[] parts = template.split("%player%");
		Component result = Component.text(parts[0], NamedTextColor.GRAY);
		if (parts.length > 1) {
			result = result
				.append(Component.text(playerName, NamedTextColor.AQUA))
				.append(Component.text(parts[1], NamedTextColor.GRAY));
		}
		return result;
	}

	private final ArrayList<String> messages = new ArrayList<>(
		List.of(
			"How dare you deliver this to me? This present is for %player%!",
			"Are you too stupid to read? The name on the present clearly says %player%!",
			"Why are you giving me this? It's clearly for %player%!",
			"I don't want this! This belongs to %player%!",
			"Is there something wrong with your eyes? This is meant for %player%!",
			"I'm not %player%! Take this to the right person!",
			"Seriously? This is for %player%, not me!",
			"You've got the wrong person! This is for %player%!",
			"Don't waste my time! This clearly says it's for %player%!",
			"I think you're lost — this is meant for %player%!",
			"Stop being careless! This is for %player%, not me!",
			"Do I look like %player% to you? Are you blind?",
			"This isn't mine — it's for %player%!",
			"Take a closer look. This belongs to %player%!",
			"How can you mix this up? It's clearly for %player%!",
			"Not my name on the present—it's %player%'s!",
			"This isn't funny. Give this to %player%!",
			"Read the label! It's for %player%!",
			"Stop bothering me and give this to %player%!",
			"I'm not the recipient! This is meant for %player%!",
			"You've made a mistake—this belongs to %player%!",
			"Clearly, you didn't read the tag. This is for %player%!"
		)
	);

	public void onReceiverClicked(Player player, String name) {
		if (
			Snowsplash.getInstance()
					.getGameManager()
					.getTeamManager()
					.getTeam(player) instanceof
				ElfTeam team
		) {
			// Make sure the guy actually has a present
			if (!currentDelivery.containsKey(player)) {
				player.sendMessage(
					Component.text(
						name,
						NamedTextColor.WHITE,
						net.kyori.adventure.text.format.TextDecoration.BOLD
					)
						.append(
							Component.text(
								": You don't even have a present! Get one from ",
								NamedTextColor.GRAY
							)
						)
						.append(Component.text("Santa ", NamedTextColor.RED))
						.append(Component.text("first.", NamedTextColor.GRAY))
				);
				return;
			}

			if (Objects.equals(currentDelivery.get(player), name)) {
				player.getInventory().remove(Material.RED_WOOL);
				presentsLeft -= 1;
				currentDelivery.remove(player);

				// Send an announcement that a present has been delivered
				Bukkit.broadcast(Component.text(" "));
				Bukkit.broadcast(
					Component.text(
						player.getName(),
						NamedTextColor.AQUA,
						net.kyori.adventure.text.format.TextDecoration.BOLD
					)
						.append(
							Component.text(" delivered a ", NamedTextColor.GRAY)
						)
						.append(Component.text("present", NamedTextColor.AQUA))
						.append(Component.text("!", NamedTextColor.GRAY))
				);
				Bukkit.broadcast(Component.text(" "));

				// Check if the win condition for the elves is met
				if (presentsLeft <= 0) {
					handleWin(team);
				}
			} else {
				var messageTemplate = messages.get(
					ThreadLocalRandom.current().nextInt(messages.size())
				);
				Component message = buildMessage(
					messageTemplate,
					currentDelivery.get(player)
				);
				player.sendMessage(
					Component.text(
						name,
						NamedTextColor.WHITE,
						net.kyori.adventure.text.format.TextDecoration.BOLD
					)
						.append(Component.text(": ", NamedTextColor.GRAY))
						.append(message)
				);
			}
		}
	}

	public void onGiverClicked(Player player) {
		if (
			Snowsplash.getInstance()
					.getGameManager()
					.getTeamManager()
					.getTeam(player) instanceof
				ElfTeam
		) {
			if (currentDelivery.containsKey(player)) {
				player.sendMessage(
					Component.text(
						"Santa",
						NamedTextColor.RED,
						net.kyori.adventure.text.format.TextDecoration.BOLD
					).append(
						Component.text(
							": I already gave you a present!",
							NamedTextColor.GRAY
						)
					)
				);
				return;
			}

			// Get a random receiver to bring the present to
			final var randomInt = ThreadLocalRandom.current().nextInt(
				receivers.size()
			);
			final var receiver = receivers
				.keySet()
				.stream()
				.toList()
				.get(randomInt);

			// Assign that receiver for the player
			currentDelivery.put(player, receiver);
			final var present = new ItemStackBuilder(Material.RED_WOOL)
				.withName(Component.text("Present", NamedTextColor.RED))
				.buildStack();
			player.getInventory().addItem(present);
			player.sendMessage(
				Component.text(
					"Santa",
					NamedTextColor.RED,
					net.kyori.adventure.text.format.TextDecoration.BOLD
				)
					.append(
						Component.text(
							": Bring this present to ",
							NamedTextColor.GRAY
						)
					)
					.append(Component.text(receiver, NamedTextColor.RED))
					.append(Component.text("!", NamedTextColor.GRAY))
			);
		}
	}

	@Override
	public void onMove(PlayerMoveEvent event) {
		if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
			return;
		}

		// Kill the player in case they fell down
		if (event.getPlayer().getLocation().getY() <= 156) {
			event.getPlayer().setHealth(0);
			return;
		}

		Team team = Snowsplash.getInstance()
			.getGameManager()
			.getTeamManager()
			.getTeam(event.getPlayer());

		// Check if they wandered into a trap
		ArrayList<DroppableTrap> toRemove = new ArrayList<>();
		for (DroppableTrap trap : traps) {
			if (
				trap.location.distance(event.getPlayer().getLocation()) <= 3 &&
				!team.getName().equals(trap.team.getName())
			) {
				toRemove.add(trap);
				trap.onEnter(event.getPlayer());
				break;
			}
		}
		for (DroppableTrap rem : toRemove) {
			rem.item.remove();
			traps.remove(rem);
		}
	}

	@Override
	public void onDamage(EntityDamageEvent event) {
		event.setCancelled(false);
	}

	@Override
	public void onDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity().getType() == EntityType.ARMOR_STAND) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		event.blockList().clear();
	}

	@Override
	public void onPlace(BlockPlaceEvent event) {
		if (event.getBlockPlaced().getType().equals(Material.REDSTONE_TORCH)) {
			event.setCancelled(true);
			return;
		}

		if (event.getBlockPlaced().getLocation().getY() >= 250) {
			event.setCancelled(true);
			return;
		}

		for (DroppableTrap trap : traps) {
			if (trap.location.distance(event.getBlock().getLocation()) <= 2) {
				event
					.getPlayer()
					.sendMessage(
						Component.text(
							"You can't place a block near a trap!",
							NamedTextColor.RED
						)
					);
				event.setCancelled(true);
				return;
			}
		}

		// Place a machine if it is one
		final var machine = Snowsplash.getInstance()
			.getMachineManager()
			.newMachineByMaterial(
				event.getBlockPlaced().getType(),
				event.getBlockPlaced().getLocation()
			);
		if (machine != null) {
			Snowsplash.getInstance().getMachineManager().addMachine(machine);
		} else {
			// Add the block as placed otherwise
			placedBlocks.put(event.getBlock().getLocation(), true);
		}
	}

	final List<Material> grassTypes = Arrays.asList(
		Material.TALL_GRASS,
		Material.SHORT_GRASS,
		Material.CORNFLOWER,
		Material.DANDELION,
		Material.POPPY,
		Material.BLUE_ORCHID,
		Material.ALLIUM,
		Material.AZURE_BLUET,
		Material.RED_TULIP,
		Material.ORANGE_TULIP,
		Material.WHITE_TULIP,
		Material.PINK_TULIP,
		Material.OXEYE_DAISY,
		Material.SUNFLOWER,
		Material.LILAC,
		Material.ROSE_BUSH,
		Material.PEONY,
		Material.LILY_OF_THE_VALLEY,
		Material.WITHER_ROSE,
		Material.COBWEB,
		Material.FERN,
		Material.SWEET_BERRY_BUSH
	);

	@Override
	public void onBreak(BlockBreakEvent event) {
		if (
			Snowsplash.getInstance()
				.getMachineManager()
				.breakLocation(event.getBlock().getLocation())
		) {
			event.setDropItems(false);
			return;
		}

		// Only let placed blocks be broken again
		if (placedBlocks.get(event.getBlock().getLocation()) != null) {
			placedBlocks.remove(event.getBlock().getLocation());
			return;
		}

		// Let grass blocks be removed permanently (for PvP)
		if (grassTypes.contains(event.getBlock().getType())) {
			event.setDropItems(false);
			event.setCancelled(false);
			return;
		}

		event.setCancelled(true);
	}

	@Override
	public void onDeath(PlayerDeathEvent event) {
		final var player = event.getPlayer();

		player.getInventory().clear();
		player.getInventory().setBoots(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setHelmet(null);
		event.deathMessage(null);
		event.setKeepLevel(true);

		if (player.getKiller() != null) {
			Bukkit.broadcast(
				Snowsplash.PREFIX.append(
					Component.text(player.getName(), NamedTextColor.AQUA)
						.append(
							Component.text(
								" was killed by ",
								NamedTextColor.GRAY
							)
						)
						.append(
							Component.text(
								player.getKiller().getName(),
								NamedTextColor.AQUA,
								net.kyori.adventure.text.format.TextDecoration.BOLD
							)
						)
						.append(Component.text("!", NamedTextColor.GRAY))
				)
			);
		} else Bukkit.broadcast(
			Snowsplash.PREFIX.append(
				Component.text(
					player.getName(),
					NamedTextColor.AQUA,
					net.kyori.adventure.text.format.TextDecoration.BOLD
				).append(Component.text(" died!", NamedTextColor.GRAY))
			)
		);

		// Make sure the player isn't still delivering
		currentDelivery.remove(player);

		Snowsplash.getInstance()
			.getTaskManager()
			.inject(
				new Runnable() {
					int tickCount = 0;

					@Override
					public void run() {
						if (tickCount++ >= 1) {
							if (player.isDead()) {
								player.spigot().respawn();
								player.getInventory().clear();
								player.setHealth(20);
							}
							Snowsplash.getInstance()
								.getTaskManager()
								.uninject(this);
						}
					}
				}
			);
	}

	@Override
	public void onRespawn(PlayerRespawnEvent event) {
		final var team = Snowsplash.getInstance()
			.getGameManager()
			.getTeamManager()
			.getTeam(event.getPlayer());
		if (team instanceof HunterTeam) {
			event.setRespawnLocation(
				Objects.requireNonNull(LocationAPI.getLocation("Hunters"))
			);
		} else {
			event.setRespawnLocation(
				Objects.requireNonNull(LocationAPI.getLocation("Elves"))
			);
		}
	}

	public void handleWin(Team team) {
		team.handleWin();
		Snowsplash.getInstance().getTaskManager().uninject(runnable);
		Snowsplash.getInstance()
			.getGameManager()
			.setCurrentState(new EndState());
	}

	@Override
	public void quit(Player player) {
		Team team = Snowsplash.getInstance()
			.getGameManager()
			.getTeamManager()
			.getTeam(player);
		team.getPlayers().remove(player);

		// Make sure the team loses if there are no players left
		if (team.getPlayers().isEmpty()) {
			if (team instanceof HunterTeam) {
				handleWin(
					Snowsplash.getInstance()
						.getGameManager()
						.getTeamManager()
						.getTeam("Elves")
				);
			} else {
				handleWin(
					Snowsplash.getInstance()
						.getGameManager()
						.getTeamManager()
						.getTeam("Hunters")
				);
			}
		}
	}

	public abstract static class DroppableTrap {

		public final Location location;
		public final Team team;
		public final Item item;
		public final long start;

		public DroppableTrap(Location location, Team team, Material material) {
			this.location = location;
			this.team = team;

			item = (Item) location
				.getWorld()
				.spawnEntity(location.clone().add(0, 0.5, 0), EntityType.ITEM);
			item.setItemStack(new ItemStackBuilder(material).buildStack());
			item.setVelocity(new Vector(0, 0, 0));
			item.setPickupDelay(1000000000);
			item.setCanPlayerPickup(false);
			item.setCanMobPickup(false);
			item.setUnlimitedLifetime(true);
			start = System.currentTimeMillis();
		}

		public abstract void onEnter(Player player);
	}

	public static class SlowTrap extends DroppableTrap {

		SlowTrap(Location location, Team team) {
			super(location, team, Material.GRAY_DYE);
		}

		@Override
		public void onEnter(Player player) {
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.SLOWNESS, 300, 4)
			);
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.GLOWING, 300, 0)
			);
		}
	}

	public static class PoisonTrap extends DroppableTrap {

		PoisonTrap(Location location, Team team) {
			super(location, team, Material.LIME_DYE);
		}

		@Override
		public void onEnter(Player player) {
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.POISON, 100, 2)
			);
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.GLOWING, 300, 0)
			);
		}
	}

	public static class FreezeTrap extends DroppableTrap {

		FreezeTrap(Location location, Team team) {
			super(location, team, Material.LIGHT_BLUE_DYE);
		}

		@Override
		public void onEnter(Player player) {
			player.addPotionEffect(
				new PotionEffect(
					PotionEffectType.SLOWNESS,
					100,
					255,
					true,
					false
				)
			);
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.GLOWING, 300, 0)
			);
		}
	}

	public static class FlyTrap extends DroppableTrap {

		FlyTrap(Location location, Team team) {
			super(location, team, Material.FEATHER);
		}

		@Override
		public void onEnter(Player player) {
			player.setVelocity(new Vector(0, 3, 0));
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.GLOWING, 300, 0)
			);
		}
	}

	public static class WebTrap extends DroppableTrap {

		WebTrap(Location location, Team team) {
			super(location, team, Material.WHITE_DYE);
		}

		@Override
		public void onEnter(Player player) {
			// Place 5 blocks of webs around the location
			final var main = location.clone().getBlock();
			main.setType(Material.COBWEB);
			main.getRelative(BlockFace.EAST).setType(Material.COBWEB);
			main.getRelative(BlockFace.WEST).setType(Material.COBWEB);
			main.getRelative(BlockFace.NORTH).setType(Material.COBWEB);
			main.getRelative(BlockFace.SOUTH).setType(Material.COBWEB);
			player.addPotionEffect(
				new PotionEffect(PotionEffectType.GLOWING, 300, 0)
			);
		}
	}
}
