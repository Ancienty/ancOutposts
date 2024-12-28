package com.ancienty.ancoutposts.GUIs;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Classes.Reward;
import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OutpostGUI {

    public static final String OUTPOST_GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&6&lOutposts");

    public static void openOutpostGUI(Player player) {
        FileConfiguration config = Main.getPlugin().getConfig();
        // Create an inventory with size 27 (can be adjusted as needed)
        Inventory gui = Bukkit.createInventory(null, 27, OUTPOST_GUI_TITLE);

        // Iterate over outposts to populate the GUI
        for (Outpost outpost : OutpostManager.getOutposts().values()) {
            String id = outpost.getId();
            ConfigurationSection outpostSection = config.getConfigurationSection("outposts." + id);
            if (outpostSection == null) continue;

            // Get slot
            int slot = outpostSection.getInt("slot", 0);
            if (slot < 0 || slot >= gui.getSize()) {
                slot = gui.firstEmpty();
                if (slot == -1) continue; // Inventory is full
            }

            // Create the item (you can customize the material per outpost if desired)
            ItemStack item = new ItemStack(Material.BEACON);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // Set display name
            String displayName = outpost.getDisplayName();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            // Get lore from config
            List<String> loreConfig = outpostSection.getStringList("lore");
            List<String> lore = new ArrayList<>();
            for (String line : loreConfig) {
                if (line.contains("{rewards}")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&8• &7Rewards:"));

                    // Add one-time rewards
                    for (Reward reward : outpost.getOneTimeRewards()) {
                        String rewardDisplay = reward.getDisplay();
                        if (rewardDisplay != null) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', rewardDisplay));
                        } else {
                            // Fallback
                            String rewardLine = "  &8- &7" + reward.getType();
                            if (reward.getPercent() != null) {
                                rewardLine += " " + reward.getPercent() + "%";
                            }
                            lore.add(ChatColor.translateAlternateColorCodes('&', rewardLine));
                        }
                    }

                    // Add recurring rewards
                    for (Reward reward : outpost.getRecurringRewards()) {
                        String rewardDisplay = reward.getDisplay();
                        if (rewardDisplay != null) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', rewardDisplay + " (Every " + reward.getInterval() + " min)"));
                        } else {
                            // Fallback
                            String rewardLine = "  &8- &7" + reward.getType();
                            if (reward.getPercent() != null) {
                                rewardLine += " " + reward.getPercent() + "%";
                            }
                            rewardLine += " (Every " + reward.getInterval() + " min)";
                            lore.add(ChatColor.translateAlternateColorCodes('&', rewardLine));
                        }
                    }

                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&', parsePlaceholders(line, outpost)));
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Place the item in the GUI
            gui.setItem(slot, item);
        }

        // Open the GUI for the player
        player.openInventory(gui);
    }

    /**
     * Parses placeholders in the lore lines.
     *
     * @param line    The line of lore to parse.
     * @param outpost The outpost associated with the lore.
     * @return The parsed line.
     */
    private static String parsePlaceholders(String line, Outpost outpost) {
        // Replace {owner}
        String owner = outpost.getOwnerClanName();
        line = line.replace("{owner}", owner);

        // Replace {progress}
        line = line.replace("{progress}", String.format("%.1f%%", outpost.getProgress()));

        return line;
    }

    /**
     * Handles click events in the Outpost GUI.
     *
     * @param event The inventory click event.
     */
    public static void handleOutpostGUIClick(InventoryClickEvent event) {
        Inventory gui = event.getInventory();
        if (gui == null || !event.getView().getTitle().equals(OUTPOST_GUI_TITLE)) return;

        event.setCancelled(true); // Prevent item movement
        event.getWhoClicked().closeInventory(); // Close the inventory
    }
}
