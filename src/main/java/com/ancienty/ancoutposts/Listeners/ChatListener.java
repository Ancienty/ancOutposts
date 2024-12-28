package com.ancienty.ancoutposts.Listeners;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Classes.OutpostCreationState;
import com.ancienty.ancoutposts.Classes.Reward;
import com.ancienty.ancoutposts.Classes.RewardCreationState;
import com.ancienty.ancoutposts.GUIs.OutpostEditorGUI;
import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Main plugin = Main.getPlugin();
        UUID playerUUID = player.getUniqueId();

        // Check for RewardCreationState
        RewardCreationState rewardState = plugin.getRewardCreationStates().get(playerUUID);
        if (rewardState != null) {
            event.setCancelled(true); // Cancel the chat message

            Bukkit.getScheduler().runTask(plugin, () -> {
                handleRewardCreationChatInput(player, event.getMessage(), rewardState);
            });
            return;
        }

        // Check for OutpostCreationState
        OutpostCreationState outpostState = plugin.getOutpostCreationStates().get(playerUUID);
        if (outpostState != null) {
            event.setCancelled(true); // Cancel the chat message

            Bukkit.getScheduler().runTask(plugin, () -> {
                handleOutpostCreationChatInput(player, event.getMessage(), outpostState);
            });
        }
    }

    private void handleRewardCreationChatInput(Player player, String message, RewardCreationState state) {
        switch (state.getStep()) {
            case 1: // Awaiting reward type
                String rewardTypeInput = message.trim().toLowerCase();
                String rewardType = null;
                if (rewardTypeInput.equals("command")) {
                    rewardType = "COMMAND";
                } else if (rewardTypeInput.equals("increase_damage") || rewardTypeInput.equals("increase damage")) {
                    rewardType = "INCREASED_DAMAGE";
                } else if (rewardTypeInput.equals("decrease_damage") || rewardTypeInput.equals("decrease damage")) {
                    rewardType = "REDUCED_DAMAGE";
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid reward type. Please enter 'command', 'increase_damage', or 'decrease_damage'.");
                    return; // Remain on step 1
                }
                state.setRewardType(rewardType);
                state.setStep(2);
                if (rewardType.equals("COMMAND")) {
                    player.sendMessage(ChatColor.YELLOW + "Please enter the command to execute (without '/'), e.g., 'eco give {player} 500':");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Please enter the percent value for the reward (e.g., '5'):");
                }
                break;
            case 2: // Awaiting percent or command
                if (state.getRewardType().equals("COMMAND")) {
                    state.setCommand(message.trim());
                    state.setStep(3);
                    player.sendMessage(ChatColor.YELLOW + "Please enter the display name for the reward (e.g., '&8• &eMoney &8- &f500$'):");
                } else {
                    try {
                        int percent = Integer.parseInt(message.trim());
                        state.setPercent(percent);
                        state.setStep(3);
                        player.sendMessage(ChatColor.YELLOW + "Please enter the display name for the reward (e.g., '&8• &2Increased Damage &8- &f5%'):");
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid percent value. Please enter a valid integer.");
                    }
                }
                break;
            case 3: // Awaiting display name
                state.setDisplay(message.trim());
                if (state.isRecurring()) {
                    state.setStep(4);
                    player.sendMessage(ChatColor.YELLOW + "Please enter the interval in minutes for the recurring reward (e.g., '30'):");
                } else {
                    createReward(player, state);
                }
                break;
            case 4: // Awaiting interval for recurring reward
                try {
                    int interval = Integer.parseInt(message.trim());
                    state.setInterval(interval);
                    createReward(player, state);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid interval value. Please enter a valid integer.");
                }
                break;
            default:
                break;
        }
    }

    private void createReward(Player player, RewardCreationState state) {
        String outpostId = state.getOutpostId();
        Outpost outpost = OutpostManager.getOutpost(outpostId);
        if (outpost != null) {
            Reward reward;
            if (state.isRecurring()) {
                reward = new Reward(
                        state.getRewardType(),
                        state.getPercent(),
                        state.getDisplay(),
                        state.getCommand(),
                        true,
                        state.getInterval()
                );
                outpost.getRecurringRewards().add(reward);
            } else {
                reward = new Reward(
                        state.getRewardType(),
                        state.getPercent(),
                        state.getDisplay(),
                        state.getCommand()
                );
                outpost.getOneTimeRewards().add(reward);
            }
            // Save the updated outpost configuration
            OutpostManager.saveOutpostConfig(outpost);
            outpost.saveState();
            player.sendMessage(ChatColor.GREEN + "Reward added successfully!");
            // Re-open the edit rewards GUI
            OutpostEditorGUI.openEditRewardsGUI(player, outpost);
        } else {
            player.sendMessage(ChatColor.RED + "Error: Outpost not found.");
        }
        // Remove the state
        Main.getPlugin().getRewardCreationStates().remove(player.getUniqueId());
    }

    private void handleOutpostCreationChatInput(Player player, String message, OutpostCreationState state) {
        Main plugin = Main.getPlugin();
        String response = message.trim().toLowerCase();
        if (response.equals("yes")) {
            // Create a new outpost config section
            String id = state.getOutpostId();
            FileConfiguration config = Main.getPlugin().getConfig();
            String path = "outposts." + id;

            config.set(path + ".name", "&6&l" + id); // Set a default name
            config.set(path + ".claim_time", config.getInt("config.claim_time", 100));
            // Save the config
            Main.getPlugin().saveConfig();

            player.sendMessage(ChatColor.GREEN + "Outpost " + ChatColor.GOLD + id + ChatColor.GREEN + " created. You can now edit it using the editor GUI.");

            // Proceed to create the Outpost object
            Map<UUID, Location[]> selections = plugin.getSelections();
            Location[] selection = selections.get(player.getUniqueId());

            if (selection == null || selection[0] == null || selection[1] == null) {
                player.sendMessage(ChatColor.RED + "You need to select two corners first using /outpost pos1 and /outpost pos2.");
                // Remove the state
                plugin.getOutpostCreationStates().remove(player.getUniqueId());
                return;
            }

            // Use default values or set as needed
            String displayName = config.getString(path + ".name", id);
            int claimTime = config.getInt(path + ".claim_time", 100);

            List<Reward> oneTimeRewards = new ArrayList<>(); // Empty list for one-time rewards
            List<Reward> recurringRewards = new ArrayList<>(); // Empty list for recurring rewards

            // Create the Outpost object
            Outpost outpost = new Outpost(id, displayName, selection[0], selection[1], claimTime, oneTimeRewards, recurringRewards);
            OutpostManager.addOutpost(id, outpost);

            // Save the corners to database.yml for persistence
            FileConfiguration db = Main.getPlugin().getDatabaseConfig();
            OutpostManager.saveLocation(db, "outposts." + id + ".corner1", selection[0]);
            OutpostManager.saveLocation(db, "outposts." + id + ".corner2", selection[1]);
            Main.getPlugin().saveDatabase();

            plugin.sendMessage(player, "outpost_created", "outpost_name", displayName, "outpost_id", id);

            // Open the editor GUI for the player
            OutpostEditorGUI.openEditorGUI(player, outpost);

        } else if (response.equals("no")) {
            player.sendMessage(ChatColor.YELLOW + "Outpost creation cancelled.");
        } else {
            player.sendMessage(ChatColor.RED + "Please answer with 'yes' or 'no'.");
            return; // Wait for a correct answer
        }
        // Remove the state
        plugin.getOutpostCreationStates().remove(player.getUniqueId());
    }
}
