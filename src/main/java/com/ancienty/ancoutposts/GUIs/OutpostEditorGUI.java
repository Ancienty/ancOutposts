package com.ancienty.ancoutposts.GUIs;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Classes.Reward;
import com.ancienty.ancoutposts.Classes.RewardCreationState;
import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutpostEditorGUI {

    public static final String EDITOR_GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&e&lEditing Outpost");
    public static final String EDIT_REWARDS_GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&e&lEdit Rewards");

    public static void openEditorGUI(Player player, Outpost outpost) {
        Inventory gui = Bukkit.createInventory(null, 9, EDITOR_GUI_TITLE);

        // Claim Time Item
        ItemStack claimTimeItem = new ItemStack(Material.WATCH);
        ItemMeta claimTimeMeta = claimTimeItem.getItemMeta();
        if (claimTimeMeta != null) {
            claimTimeMeta.setDisplayName(ChatColor.YELLOW + "Edit Claim Time");
            claimTimeMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current: " + outpost.getClaimTime() + " seconds",
                    ChatColor.GREEN + "Click to increase by 10 seconds",
                    ChatColor.RED + "Right-click to decrease by 10 seconds"
            ));
            claimTimeItem.setItemMeta(claimTimeMeta);
        }
        gui.setItem(1, claimTimeItem);

        // Edit Rewards Item
        ItemStack rewardsItem = new ItemStack(Material.DIAMOND);
        ItemMeta rewardsMeta = rewardsItem.getItemMeta();
        if (rewardsMeta != null) {
            rewardsMeta.setDisplayName(ChatColor.YELLOW + "Edit Rewards");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to edit rewards");
            rewardsMeta.setLore(lore);
            rewardsItem.setItemMeta(rewardsMeta);
        }
        gui.setItem(3, rewardsItem);

        // Store the outpost being edited in the player's metadata
        player.setMetadata("editingOutpost", new FixedMetadataValue(Main.getPlugin(), outpost.getId()));

        // Open the GUI
        player.openInventory(gui);
    }

    public static void openEditRewardsGUI(Player player, Outpost outpost) {
        Inventory gui = Bukkit.createInventory(null, 9, EDIT_REWARDS_GUI_TITLE);

        // Add One-Time Reward Button
        ItemStack addOneTimeRewardItem = new ItemStack(Material.EMERALD);
        ItemMeta addOneTimeRewardMeta = addOneTimeRewardItem.getItemMeta();
        if (addOneTimeRewardMeta != null) {
            addOneTimeRewardMeta.setDisplayName(ChatColor.GREEN + "Add One-Time Reward");
            addOneTimeRewardMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to add a new one-time reward"
            ));
            addOneTimeRewardItem.setItemMeta(addOneTimeRewardMeta);
        }
        gui.setItem(2, addOneTimeRewardItem);

        // Add Recurring Reward Button
        ItemStack addRecurringRewardItem = new ItemStack(Material.EMERALD);
        ItemMeta addRecurringRewardMeta = addRecurringRewardItem.getItemMeta();
        if (addRecurringRewardMeta != null) {
            addRecurringRewardMeta.setDisplayName(ChatColor.GREEN + "Add Recurring Reward");
            addRecurringRewardMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to add a new recurring reward"
            ));
            addRecurringRewardItem.setItemMeta(addRecurringRewardMeta);
        }
        gui.setItem(4, addRecurringRewardItem);

        // Remove Reward Button
        ItemStack removeRewardItem = new ItemStack(Material.REDSTONE);
        ItemMeta removeRewardMeta = removeRewardItem.getItemMeta();
        if (removeRewardMeta != null) {
            removeRewardMeta.setDisplayName(ChatColor.RED + "Remove Last Reward");
            removeRewardMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to remove the last one-time reward",
                    ChatColor.GRAY + "Shift-click to remove the last recurring reward"
            ));
            removeRewardItem.setItemMeta(removeRewardMeta);
        }
        gui.setItem(6, removeRewardItem);

        // Store the outpost being edited in the player's metadata
        player.setMetadata("editingOutpost", new FixedMetadataValue(Main.getPlugin(), outpost.getId()));

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Handles click events in the editor GUI.
     *
     * @param player The player who clicked.
     * @param slot   The slot clicked.
     * @param click  The click type.
     */
    public static void handleEditorGUIClick(Player player, int slot, ClickType click) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (gui == null) {
            return;
        }

        String title = gui.getTitle();

        if (title.equals(EDITOR_GUI_TITLE)) {
            // Handle clicks in the main editor GUI
            handleMainEditorGUIClick(player, slot, click);
        } else if (title.equals(EDIT_REWARDS_GUI_TITLE)) {
            // Handle clicks in the edit rewards GUI
            handleEditRewardsGUIClick(player, slot, click);
        }
    }

    private static void handleMainEditorGUIClick(Player player, int slot, ClickType click) {
        if (!player.hasMetadata("editingOutpost")) {
            return;
        }
        String outpostId = player.getMetadata("editingOutpost").get(0).asString();
        Outpost outpost = OutpostManager.getOutpost(outpostId);
        if (outpost == null) {
            return;
        }

        switch (slot) {
            case 1: // Claim Time
                int timeChange = click.isLeftClick() ? 10 : -10;
                int newClaimTime = outpost.getClaimTime() + timeChange;
                if (newClaimTime < 10) newClaimTime = 10; // Minimum claim time
                outpost.setClaimTime(newClaimTime);
                Main.getPlugin().sendMessage(player, "claim_time_set", "claim_time", String.valueOf(outpost.getClaimTime()));
                break;
            case 3: // Edit Rewards
                openEditRewardsGUI(player, outpost);
                return; // Do not reopen the main GUI
            default:
                break;
        }

        // Save the updated claim time to the config
        OutpostManager.saveOutpostConfig(outpost);
        // Re-open the editor GUI to reflect changes
        openEditorGUI(player, outpost);
        // Save the dynamic state if needed
        outpost.saveState();
    }

    private static void handleEditRewardsGUIClick(Player player, int slot, ClickType click) {
        if (!player.hasMetadata("editingOutpost")) {
            return;
        }
        String outpostId = player.getMetadata("editingOutpost").get(0).asString();
        Outpost outpost = OutpostManager.getOutpost(outpostId);
        if (outpost == null) {
            return;
        }

        switch (slot) {
            case 2: // Add One-Time Reward
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Please type the reward type in chat (command, increase_damage, decrease_damage):");
                RewardCreationState oneTimeState = new RewardCreationState(outpostId);
                oneTimeState.setRecurring(false);
                Main.getPlugin().getRewardCreationStates().put(player.getUniqueId(), oneTimeState);
                break;
            case 4: // Add Recurring Reward
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Please type the reward type in chat (command, increase_damage, decrease_damage):");
                RewardCreationState recurringState = new RewardCreationState(outpostId);
                recurringState.setRecurring(true);
                Main.getPlugin().getRewardCreationStates().put(player.getUniqueId(), recurringState);
                break;
            case 6: // Remove Last Reward
                if (click.isShiftClick()) {
                    // Remove from recurring rewards
                    if (!outpost.getRecurringRewards().isEmpty()) {
                        Reward removedReward = outpost.getRecurringRewards().remove(outpost.getRecurringRewards().size() - 1);
                        Main.getPlugin().sendMessage(player, "reward_removed", "reward_type", removedReward.getType());
                    } else {
                        Main.getPlugin().sendMessage(player, "no_rewards_to_remove");
                    }
                } else {
                    // Remove from one-time rewards
                    if (!outpost.getOneTimeRewards().isEmpty()) {
                        Reward removedReward = outpost.getOneTimeRewards().remove(outpost.getOneTimeRewards().size() - 1);
                        Main.getPlugin().sendMessage(player, "reward_removed", "reward_type", removedReward.getType());
                    } else {
                        Main.getPlugin().sendMessage(player, "no_rewards_to_remove");
                    }
                }
                // Save the updated rewards to the config
                OutpostManager.saveOutpostConfig(outpost);
                // Re-open the edit rewards GUI to reflect changes
                openEditRewardsGUI(player, outpost);
                outpost.saveState();
                break;
            default:
                break;
        }
    }
}
