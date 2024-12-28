package com.ancienty.ancoutposts.Commands;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Classes.OutpostCreationState;
import com.ancienty.ancoutposts.Classes.Reward;
import com.ancienty.ancoutposts.GUIs.AdminGUI;
import com.ancienty.ancoutposts.GUIs.OutpostEditorGUI;
import com.ancienty.ancoutposts.GUIs.OutpostGUI;
import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class OutpostCommand implements CommandExecutor {

    private final Main plugin;

    public OutpostCommand(Main plugin) {
        this.plugin = plugin;
        plugin.getCommand("outpost").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // If no arguments are provided, open the GUI
        if (args.length == 0) {
            OutpostGUI.openOutpostGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.isOp()) {
                plugin.sendMessage(player, "no_permission");
                return true;
            }
            AdminGUI.openAdminGUI(player);
            return true;
        }

        if (!player.isOp()) {
            plugin.sendMessage(player, "no_permission");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            OutpostManager.reloadOutposts();
            plugin.sendMessage(player, "config_reloaded");
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(player, "usage", "usage", "/outpost <create|pos1|pos2|admin> [id]");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                plugin.sendMessage(player, "usage", "usage", "/outpost create <id>");
                return true;
            }

            String id = args[1];

            Map<UUID, Location[]> selections = plugin.getSelections();
            Location[] selection = selections.get(player.getUniqueId());
            if (selection == null || selection[0] == null || selection[1] == null) {
                plugin.sendMessage(player, "need_select_corners");
                return true;
            }

            FileConfiguration config = Main.getPlugin().getConfig();
            if (!config.contains("outposts." + id)) {
                // Ask the player if they want to create a new outpost with this ID
                player.sendMessage(ChatColor.YELLOW + "Do you wish to create a new Outpost called " + ChatColor.GOLD + id + ChatColor.YELLOW + "? Type 'yes' or 'no'.");
                // Store the player's state
                OutpostCreationState state = new OutpostCreationState(id);
                plugin.getOutpostCreationStates().put(player.getUniqueId(), state);
                return true;
            }

            // Existing code to create the outpost if it already exists in config.yml
            String displayName = config.getString("outposts." + id + ".name", id);
            int claimTime = config.getInt("outposts." + id + ".claim_time", config.getInt("config.claim_time", 100));

            // Load rewards
            List<Reward> oneTimeRewards = new ArrayList<>();
            List<Reward> recurringRewards = new ArrayList<>();

            ConfigurationSection rewardsSection = config.getConfigurationSection("outposts." + id + ".rewards");
            if (rewardsSection != null) {
                // Load one-time rewards
                ConfigurationSection oneTimeSection = rewardsSection.getConfigurationSection("one-time");
                if (oneTimeSection != null) {
                    for (String key : oneTimeSection.getKeys(false)) {
                        ConfigurationSection rewardSection = oneTimeSection.getConfigurationSection(key);
                        String type = rewardSection.getString("type");
                        Integer percent = rewardSection.contains("percent") ? rewardSection.getInt("percent") : null;
                        String display = rewardSection.getString("display");
                        String command = rewardSection.getString("command");
                        Reward reward = new Reward(type, percent, display, command);
                        oneTimeRewards.add(reward);
                    }
                }
                // Load recurring rewards
                ConfigurationSection recurringSection = rewardsSection.getConfigurationSection("recurring");
                if (recurringSection != null) {
                    for (String key : recurringSection.getKeys(false)) {
                        ConfigurationSection rewardSection = recurringSection.getConfigurationSection(key);
                        String type = rewardSection.getString("type");
                        Integer percent = rewardSection.contains("percent") ? rewardSection.getInt("percent") : null;
                        String display = rewardSection.getString("display");
                        String command = rewardSection.getString("command");
                        boolean isRecurring = true;
                        Integer interval = rewardSection.getInt("interval");
                        Reward reward = new Reward(type, percent, display, command, isRecurring, interval);
                        recurringRewards.add(reward);
                    }
                }
            }

            // Create the Outpost object with the updated constructor
            Outpost outpost = new Outpost(id, displayName, selection[0], selection[1], claimTime, oneTimeRewards, recurringRewards);
            OutpostManager.addOutpost(id, outpost);

            // Save the corners to database.yml for persistence
            FileConfiguration db = Main.getPlugin().getDatabaseConfig();
            OutpostManager.saveLocation(db, "outposts." + id + ".corner1", selection[0]);
            OutpostManager.saveLocation(db, "outposts." + id + ".corner2", selection[1]);
            Main.getPlugin().saveDatabase();

            plugin.sendMessage(player, "outpost_created", "outpost_name", displayName, "outpost_id", id);
            return true;
        }

        if (args[0].equalsIgnoreCase("pos1")) {
            plugin.getSelections().putIfAbsent(player.getUniqueId(), new Location[2]);
            plugin.getSelections().get(player.getUniqueId())[0] = player.getLocation();
            plugin.sendMessage(player, "position_set", "pos", "1");
            return true;
        }

        if (args[0].equalsIgnoreCase("pos2")) {
            plugin.getSelections().putIfAbsent(player.getUniqueId(), new Location[2]);
            plugin.getSelections().get(player.getUniqueId())[1] = player.getLocation();
            plugin.sendMessage(player, "position_set", "pos", "2");
            return true;
        }

        plugin.sendMessage(player, "unknown_command", "usage", "/outpost <create|pos1|pos2|admin> [id]");
        return true;
    }
}
