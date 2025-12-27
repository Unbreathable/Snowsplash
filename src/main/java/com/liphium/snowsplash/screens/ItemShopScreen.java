package com.liphium.snowsplash.screens;

import com.liphium.core.inventory.CClickEvent;
import com.liphium.core.inventory.CItem;
import com.liphium.core.inventory.CScreen;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.snowsplash.Snowsplash;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ItemShopScreen extends CScreen {

    public ItemShopScreen() {
        super(3, Component.text("Item shop", NamedTextColor.DARK_AQUA, TextDecoration.BOLD), 4, false);
    }

    @Override
    public void init(Player player, Inventory inventory) {
        background(player);

        // Add all the categories
        for (ShopCategory category : ShopCategory.values()) {
            setItemNotCached(player, 10 + category.ordinal(), new CItem(category.getStack()).onClick(event -> openCategory(event, category, inventory)));
        }
    }

    public void openCategory(CClickEvent event, ShopCategory category, Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (category.getItems().size() <= i) {
                setItemNotCached(event.player(), 18 + i, ShopCategory.spacer(), inventory);
            } else {
                setItemNotCached(event.player(), 18 + i, category.getItems().get(i), inventory);
            }
        }
    }

    public static void removeAmountFromInventory(Player player, Material material, int amount) {
        int count = amount;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                int sub = Math.min(item.getAmount(), count);
                int newAmount = item.getAmount() - sub;
                item.setAmount(newAmount);
                count -= sub;
                if (count <= 0) {
                    break;
                }
            }
        }
    }

    public enum ShopCategory {
        WEAPONS(new ItemStackBuilder(Material.IRON_SWORD).withName(Component.text("Weapons", NamedTextColor.RED, TextDecoration.BOLD)).withLore(Component.text("Swords, bows, and more.", NamedTextColor.GRAY)).buildStack(), List.of(itemWithPrice(Material.MACE, "Mace", NamedTextColor.RED, 25, 1), itemWithPrice(Material.WIND_CHARGE, "Wind charge", NamedTextColor.RED, 20, 5), itemWithPrice(Material.BOW, "Bow", NamedTextColor.RED, 10, 1), itemWithPriceCustom(new ItemStackBuilder(Material.BOW).withName(Component.text("Punch Bow", NamedTextColor.RED)).withEnchantments(Map.of(Enchantment.PUNCH, 1)).buildStack(), 20), itemWithPriceCustom(new ItemStackBuilder(Material.BOW).withName(Component.text("More Punch Bow", NamedTextColor.RED)).withEnchantments(Map.of(Enchantment.PUNCH, 2, Enchantment.INFINITY, 1)).buildStack(), 100), itemWithPrice(Material.ARROW, "Arrow", NamedTextColor.RED, 15, 3), itemWithPrice(Material.CROSSBOW, "Crossbow", NamedTextColor.RED, 50, 1), itemWithPriceCustom(new ItemStackBuilder(Material.FIREWORK_ROCKET).withName(Component.text("Rocket", NamedTextColor.RED)).withAmount(3).buildStack(), 30))), DEFENSE(new ItemStackBuilder(Material.SHIELD).withName(Component.text("Defense", NamedTextColor.AQUA, TextDecoration.BOLD)).withLore(Component.text("Turrets and traps.", NamedTextColor.GRAY)).buildStack(), List.of(itemWithPrice(Material.GRAY_DYE, "Slow trap", NamedTextColor.AQUA, 25, 1), itemWithPrice(Material.LIME_DYE, "Poison trap", NamedTextColor.AQUA, 25, 1), itemWithPrice(Material.WHITE_DYE, "Web trap", NamedTextColor.AQUA, 35, 1), itemWithPrice(Material.LIGHT_BLUE_DYE, "Freeze trap", NamedTextColor.AQUA, 35, 1))), TOOLS(new ItemStackBuilder(Material.DIAMOND_PICKAXE).withName(Component.text("Tools", NamedTextColor.AQUA, TextDecoration.BOLD)).withLore(Component.text("Shovels and pickaxes.", NamedTextColor.GRAY)).buildStack(), List.of(itemWithPriceCustom(new ItemStackBuilder(Material.GOLDEN_SHOVEL).withName(Component.text("Golden shovel", NamedTextColor.AQUA)).withEnchantments(Map.of(Enchantment.EFFICIENCY, 5)).buildStack(), 15), itemWithPriceCustom(new ItemStackBuilder(Material.DIAMOND_SHOVEL).withName(Component.text("Diamond shovel", NamedTextColor.AQUA)).withEnchantments(Map.of(Enchantment.EFFICIENCY, 5)).buildStack(), 35), spacer(), itemWithPriceCustom(new ItemStackBuilder(Material.GOLDEN_PICKAXE).withName(Component.text("Golden pickaxe", NamedTextColor.AQUA)).withEnchantments(Map.of(Enchantment.EFFICIENCY, 5)).buildStack(), 15), itemWithPriceCustom(new ItemStackBuilder(Material.DIAMOND_PICKAXE).withName(Component.text("Diamond pickaxe", NamedTextColor.AQUA)).withEnchantments(Map.of(Enchantment.EFFICIENCY, 5)).buildStack(), 35))), ITEMS(new ItemStackBuilder(Material.IRON_PICKAXE).withName(Component.text("Items", NamedTextColor.WHITE, TextDecoration.BOLD)).withLore(Component.text("Blocks & materials.", NamedTextColor.GRAY)).buildStack(), List.of(itemWithPrice(Material.PACKED_ICE, "Ice", NamedTextColor.WHITE, 2, 16), itemWithPrice(Material.SNOW_BLOCK, "Snow", NamedTextColor.WHITE, 4, 16), itemWithPrice(Material.SPRUCE_LOG, "Spruce wood", NamedTextColor.WHITE, 8, 4), itemWithPrice(Material.COBBLESTONE, "Cobblestone", NamedTextColor.WHITE, 8, 16), itemWithPrice(Material.COBWEB, "Cobweb", NamedTextColor.WHITE, 10, 1), spacer(), itemWithPrice(Material.IRON_INGOT, "Iron", NamedTextColor.WHITE, 4, 1), itemWithPrice(Material.DIAMOND, "Diamond", NamedTextColor.WHITE, 7, 1), itemWithPrice(Material.GOLDEN_APPLE, "Golden apple", NamedTextColor.WHITE, 10, 1))), DROPPER(new ItemStackBuilder(Material.DROPPER).withName(Component.text("Droppers", NamedTextColor.GOLD, TextDecoration.BOLD)).withLore(Component.text("Coin and material droppers.", NamedTextColor.GRAY)).buildStack(), List.of(itemWithPrice(Material.GOLD_ORE, "Coin dropper", NamedTextColor.GOLD, 35, 1), itemWithPrice(Material.WHITE_CONCRETE, "Iron dropper", NamedTextColor.GOLD, 20, 1), itemWithPrice(Material.RED_CONCRETE, "Redstone dropper", NamedTextColor.GOLD, 20, 1), itemWithPrice(Material.CYAN_CONCRETE, "Diamond dropper", NamedTextColor.GOLD, 30, 1), itemWithPrice(Material.DISPENSER, "Dropper dropper", NamedTextColor.GOLD, 60, 1), itemWithPrice(Material.TARGET, "Arrow dropper", NamedTextColor.GOLD, 20, 1), itemWithPrice(Material.REDSTONE_LAMP, "Rocket dropper", NamedTextColor.GOLD, 20, 1), itemWithPrice(Material.BEACON, "Golden apple dropper", NamedTextColor.GOLD, 40, 1), itemWithPrice(Material.BREWING_STAND, "Brewer", NamedTextColor.GOLD, 40, 1)));

        final ItemStack stack;
        final List<CItem> items;

        ShopCategory(ItemStack stack, List<CItem> items) {
            this.stack = stack;
            this.items = items;
        }

        public ItemStack getStack() {
            return stack;
        }

        public List<CItem> getItems() {
            return items;
        }

        private static final ItemStack item = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(Component.empty()).buildStack();

        public static CItem spacer() {
            return new CItem(item).notClickable();
        }

        public static CItem itemWithPrice(Material material, String name, NamedTextColor color, int price, int amount) {
            return new CItem(new ItemStackBuilder(material).withName(Component.text(name, color)).withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD))).withAmount(amount).buildStack()).onClick(event -> buyFunction(event, new ItemStackBuilder(material).withName(Component.text(name, color)).withAmount(amount).buildStack(), price));
        }

        public static CItem itemWithPriceCustom(ItemStack sold, int price) {
            return new CItem(new ItemStackBuilder(sold.getType()).withName(sold.getItemMeta().displayName()).withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD))).withEnchantments(sold.getEnchantments()).buildStack()).onClick(event -> buyFunction(event, sold, price));
        }

        public static void buyFunction(CClickEvent event, ItemStack stack, int price) {
            // Get the amount of pumpkins in the inventory
            int count = 0;
            for (ItemStack item : event.player().getInventory()) {
                if (item != null && item.getType() == Material.GOLD_NUGGET) {
                    count += item.getAmount();
                }
            }

            if (count < price) {
                event.player().sendMessage(Snowsplash.PREFIX.append(Component.text("You don't have enough coins to purchase this item.", NamedTextColor.RED)));
                event.player().closeInventory();
                return;
            }

            // Remove the pumpkins from the players inventory
            removeAmountFromInventory(event.player(), Material.GOLD_NUGGET, price);

            event.player().getInventory().addItem(stack);
        }
    }
}
