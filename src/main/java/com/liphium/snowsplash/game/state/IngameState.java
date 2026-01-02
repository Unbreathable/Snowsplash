package com.liphium.snowsplash.game.state;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.GameState;
import com.liphium.snowsplash.game.team.Team;
import com.liphium.snowsplash.listener.machines.impl.DestroyableSnowman;
import com.liphium.snowsplash.util.LocationAPI;
import com.liphium.snowsplash.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class IngameState extends GameState {

    private final ArrayList<DroppableTrap> traps = new ArrayList<>();

    private Runnable runnable;

    private static final double SNOWMAN_HEALTH = 20;
    private static final int ICE_ON_DEATH = 2; // The amount of ice a player gets when they kill someone

    private static final int SNOWBALL_TICKS = 1;
    private static final int SNOWBALL_COOLDOWN = 6; // Cooldown that is set when players use a snowball
    private static final double SNOWBALL_DAMAGE = 2.5;

    private static final int ARROW_COOLDOWN = 70;

    public IngameState() {
        super("In game", 30);
    }

    private final HashMap<String, DestroyableSnowman> snowmen = new HashMap<>();
    private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();

    @Override
    public void start() {

        // World cleanup
        for (Entity entity : Objects.requireNonNull(Bukkit.getWorld(Snowsplash.GAME_WORLD)).getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.PLAYER) entity.remove();
        }

        // Place all the snowmen
        for(Team team : Snowsplash.getInstance().getGameManager().getTeamManager().getTeams()) {
            final var location = LocationAPI.getLocation(team.getName() + "-Snowman");
            final var man = new DestroyableSnowman(location, team, SNOWMAN_HEALTH);

            Snowsplash.getInstance().getMachineManager().addMachine(man);
            snowmen.put(team.getName(), man);
        }

        // Initialize all the teams
        for (Team team : Snowsplash.getInstance().getGameManager().getTeamManager().getTeams()) {
            team.sendStartMessage();

            for (Player player : team.getPlayers()) {
                player.getInventory().clear();
                player.setHealth(20);
                player.setFoodLevel(20);
                team.giveKit(player, true);
            }
        }

        // Start the game loop
        Snowsplash.getInstance().getTaskManager().inject(runnable = new Runnable() {
            int tickCount = 0;
            int snowballCount = 0;

            @Override
            public void run() {
                Snowsplash.getInstance().getGameManager().getTeamManager().tick();
                Snowsplash.getInstance().getMachineManager().tick();

                if (tickCount++ >= 20) {
                    tickCount = 0;

                    // Create an action bar with a health bar for the snowmen of all teams
                    var base = Component.text("");
                    var index = 0;
                    for(Team team : Snowsplash.getInstance().getGameManager().getTeamManager().getTeams()) {
                        base = base.append(snowmen.get(team.getName()).colorWithHealth("■■■■■■■■", team.getColor(), NamedTextColor.GRAY));
                        if(index != Snowsplash.getInstance().getGameManager().getTeamManager().getTeams().size() - 1) {
                            base = base.appendSpace().append(Component.text("|", NamedTextColor.DARK_GRAY)).appendSpace();
                        }
                        index++;
                    }

                    // Let a team win the game when the snowman is down
                    for(Team team : Snowsplash.getInstance().getGameManager().getTeamManager().getTeams()) {
                        final var man = snowmen.get(team.getName());

                        if(man.man.isDead()) {
                            final var other = Snowsplash.getInstance().getGameManager().getTeamManager().getTeams().stream()
                                    .filter(t -> !t.getName().equals(team.getName())).findFirst().get();
                            handleWin(other);
                        }
                    }

                    Messages.actionBar(base);
                }

                // Give players snowballs every few ticks
                if(snowballCount++ >= SNOWBALL_TICKS) {
                    snowballCount = 0;

                    for(Team team : Snowsplash.getInstance().getGameManager().getTeamManager().getTeams()) {
                        for(Player player : team.getPlayers()) {
                            player.getInventory().setItem(0, new ItemStackBuilder(Material.SNOWBALL).withAmount(16).buildStack());
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && (event.getItem().getType() == Material.WIND_CHARGE || event.getItem().getType() == Material.SNOWBALL)) {
            return;
        }

        Snowsplash.getInstance().getMachineManager().onInteract(event);

        if (event.getItem() != null) {
            Team team = Snowsplash.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
            ItemStack usedItem = event.getItem();

            if (usedItem.getType().equals(Material.GRAY_DYE) && event.getClickedBlock() != null) {
                traps.add(new SlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer(), Material.GRAY_DYE);
            } else if (usedItem.getType().equals(Material.LIME_DYE) && event.getClickedBlock() != null) {
                traps.add(new PoisonTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer(), Material.LIME_DYE);
            } else if (usedItem.getType().equals(Material.FEATHER) && event.getClickedBlock() != null) {
                traps.add(new FlyTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer(), Material.FEATHER);
            } else if (usedItem.getType().equals(Material.LIGHT_BLUE_DYE) && event.getClickedBlock() != null) {
                traps.add(new FreezeTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer(), Material.LIGHT_BLUE_DYE);
            } else if (usedItem.getType().equals(Material.WHITE_DYE) && event.getClickedBlock() != null) {
                traps.add(new WebTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer(), Material.WHITE_DYE);
            }
        }
    }

    void reduceMainHandItem(Player player, Material material) {
        if (player.getInventory().getItemInMainHand().getType() == material) {
            int amount = player.getInventory().getItemInMainHand().getAmount();
            if (amount == 1) {
                player.getInventory().setItemInMainHand(null);
            } else player.getInventory().getItemInMainHand().setAmount(amount - 1);
        } else if (player.getInventory().getItemInOffHand().getType() == material) {
            int amount = player.getInventory().getItemInOffHand().getAmount();
            if (amount == 1) {
                player.getInventory().setItemInOffHand(null);
            } else player.getInventory().getItemInOffHand().setAmount(amount - 1);
        }
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Snowsplash.getInstance().getMachineManager().onInteractAtEntity(event);

        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Team team = Snowsplash.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());

        // Check if they wandered into a trap
        ArrayList<DroppableTrap> toRemove = new ArrayList<>();
        for (DroppableTrap trap : traps) {
            if (trap.location.distance(event.getPlayer().getLocation()) <= 3 && !team.getName().equals(trap.team.getName())) {
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
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if(event.getEntity().getShooter() == null || !(event.getEntity().getShooter() instanceof Player player)) return;

        // Handle the snowball and arrow cooldowns
        if(event.getEntity().getType() == EntityType.SNOWBALL) {
            player.setCooldown(Material.SNOWBALL, SNOWBALL_COOLDOWN);
        } else if(event.getEntity().getType() == EntityType.ARROW) {
            player.setCooldown(Material.CROSSBOW, ARROW_COOLDOWN);
            player.setCooldown(Material.BOW, ARROW_COOLDOWN);
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
                event.getPlayer().sendMessage(Component.text("You can't place a block near a trap!", NamedTextColor.RED));
                event.setCancelled(true);
                return;
            }
        }

        // Instantly light TNT
        if(event.getBlock().getType() == Material.TNT) {
            event.getBlock().setType(Material.AIR);
            final var world = event.getBlock().getWorld();
            world.spawnEntity(event.getBlock().getLocation(), EntityType.TNT);
            return;
        }

        placedBlocks.put(event.getBlock().getLocation(), true);
    }

    final List<Material> grassTypes = Arrays.asList(Material.TALL_GRASS, Material.SHORT_GRASS, Material.CORNFLOWER, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.COBWEB, Material.FERN, Material.SWEET_BERRY_BUSH);

    @Override
    public void onBreak(BlockBreakEvent event) {
        if (Snowsplash.getInstance().getMachineManager().breakLocation(event.getBlock().getLocation())) {
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

        event.deathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        if (player.getKiller() != null) {
            Bukkit.broadcast(Snowsplash.PREFIX.append(Component.text(player.getName(), NamedTextColor.AQUA)
                    .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                    .append(Component.text(player.getKiller().getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.GRAY))));
            player.getKiller().getInventory().addItem(new ItemStackBuilder(Material.BLUE_ICE).withAmount(ICE_ON_DEATH).buildStack());
        } else {
            Bukkit.broadcast(Snowsplash.PREFIX
                    .append(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" died!", NamedTextColor.GRAY)));
        }

        Snowsplash.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {
                if (tickCount++ >= 1) {
                    if (player.isDead()) {
                        player.spigot().respawn();
                        player.setHealth(20);
                    }
                    Snowsplash.getInstance().getTaskManager().uninject(this);
                }
            }
        });
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        if(!(event.getEntity().getType() == EntityType.SNOWBALL)) return;

        if(event.getHitEntity() instanceof LivingEntity target) {
            target.damage(SNOWBALL_DAMAGE);

            // Apply knockback similar to vanilla
            if(target instanceof Player) {
                Vector knockback = event.getEntity().getVelocity().normalize().multiply(0.5);
                knockback.setY(0.4);
                target.setVelocity(target.getVelocity().add(knockback));
            }
        }
    }

    @Override
    public void onRespawn(PlayerRespawnEvent event) {
        final var team = Snowsplash.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
        event.setRespawnLocation(Objects.requireNonNull(LocationAPI.getLocation(team.getName())));
    }

    public void handleWin(Team team) {
        team.handleWin();
        Snowsplash.getInstance().getTaskManager().uninject(runnable);
        Snowsplash.getInstance().getGameManager().setCurrentState(new EndState());
    }

    @Override
    public void quit(Player player) {
        Team team = Snowsplash.getInstance().getGameManager().getTeamManager().getTeam(player);
        team.getPlayers().remove(player);

        // Make sure the team loses if there are no players left
        if (team.getPlayers().isEmpty()) {
            handleWin(Snowsplash.getInstance().getGameManager().getTeamManager().getTeams().stream()
                    .filter(team1 -> !team1.equals(team)).findFirst().get());
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

            item = (Item) location.getWorld().spawnEntity(location.clone().add(0, 0.5, 0), EntityType.ITEM);
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
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class PoisonTrap extends DroppableTrap {

        PoisonTrap(Location location, Team team) {
            super(location, team, Material.LIME_DYE);
        }

        @Override
        public void onEnter(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class FreezeTrap extends DroppableTrap {

        FreezeTrap(Location location, Team team) {
            super(location, team, Material.LIGHT_BLUE_DYE);
        }

        @Override
        public void onEnter(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class FlyTrap extends DroppableTrap {

        FlyTrap(Location location, Team team) {
            super(location, team, Material.FEATHER);
        }

        @Override
        public void onEnter(Player player) {
            player.setVelocity(new Vector(0, 3, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
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
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }
}
