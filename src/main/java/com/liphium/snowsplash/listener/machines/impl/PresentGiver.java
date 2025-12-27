package com.liphium.snowsplash.listener.machines.impl;

import com.liphium.snowsplash.Snowsplash;
import com.liphium.snowsplash.game.state.IngameState;
import com.liphium.snowsplash.listener.machines.Machine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.UUID;

public class PresentGiver extends Machine {

    private UUID uniqueId;

    public PresentGiver(Location location) {
        super(location, false);

        ArmorStand stand = location.getWorld().spawn(location.clone(), ArmorStand.class);

        stand.setCustomNameVisible(true);
        stand.customName(Component.text("Santa", NamedTextColor.RED, TextDecoration.BOLD));
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setRemoveWhenFarAway(false);
        stand.setBasePlate(false);

        // Give them red armor (similar to Santa ig)
        stand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta chestplateMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestplateMeta.setColor(Color.fromRGB(255, 0, 0));
        chestplate.setItemMeta(chestplateMeta);
        LeatherArmorMeta leggingsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        leggingsMeta.setColor(Color.fromRGB(255, 0, 0));
        leggings.setItemMeta(leggingsMeta);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.fromRGB(255, 0, 0));
        boots.setItemMeta(bootsMeta);
        stand.getEquipment().setChestplate(chestplate);
        stand.getEquipment().setLeggings(leggings);
        stand.getEquipment().setBoots(boots);

        uniqueId = stand.getUniqueId();
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        final var stand = (ArmorStand) location.getWorld().getEntity(uniqueId);
        if(stand == null) {
            return;
        }

        if (event.getRightClicked().equals(stand)) {
            if(Snowsplash.getInstance().getGameManager().getCurrentState() instanceof IngameState state) {
                state.onGiverClicked(event.getPlayer());
            }
            event.setCancelled(true);
        }
    }

    @Override
    public void destroy() {
        final var stand = (ArmorStand) location.getWorld().getEntity(uniqueId);
        if(stand == null) {
            return;
        }
        stand.remove();
    }
}
