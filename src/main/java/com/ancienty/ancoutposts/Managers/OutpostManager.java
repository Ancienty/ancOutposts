package com.ancienty.ancoutposts.Managers;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Classes.Reward;
import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Utils.ClanUtils;
import dev.risas.neron.models.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class OutpostManager {

    private static Map<String, Outpost> outposts = new HashMap<>();
    private static Map<Clan, List<Outpost>> claimedOutposts = new HashMap<>();

    public static void addClaimedOutpost(Clan clan, Outpost outpost) {
        claimedOutposts.computeIfAbsent(clan, k -> new ArrayList<>()).add(outpost);
    }

    public static void removeClaimedOutpost(Clan clan, Outpost outpost) {
        List<Outpost> outpostList = claimedOutposts.get(clan);
        if (outpostList != null) {
            outpostList.remove(outpost);
            if (outpostList.isEmpty()) {
                claimedOutposts.remove(clan);
            }
        }
    }

    public static List<Outpost> getClaimedOutposts(Clan clan) {
        return claimedOutposts.getOrDefault(clan, Collections.emptyList());
    }

    public static void loadOutposts() {
        FileConfiguration config = Main.getPlugin().getConfig();
        FileConfiguration db = Main.getPlugin().getDatabaseConfig();

        ConfigurationSection outpostsSection = config.getConfigurationSection("outposts");
        if (outpostsSection == null) return;

        for (String id : outpostsSection.getKeys(false)) {
            ConfigurationSection outpostSection = outpostsSection.getConfigurationSection(id);
            String displayName = outpostSection.getString("name", id);

            // Load corners from database.yml using helper method
            Location corner1 = loadLocation(db, "outposts." + id + ".corner1");
            Location corner2 = loadLocation(db, "outposts." + id + ".corner2");

            // If corners are not set, skip this outpost
            if (corner1 == null || corner2 == null) continue;

            // Load claim time
            int claimTime = outpostSection.getInt("claim_time", config.getInt("config.claim_time", 100));

            // Load rewards
            List<Reward> oneTimeRewards = new ArrayList<>();
            List<Reward> recurringRewards = new ArrayList<>();
            ConfigurationSection rewardsSection = outpostSection.getConfigurationSection("rewards");
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

            // Load dynamic data from database.yml
            double progress = db.getDouble("outposts." + id + ".progress", 0.0);

            // Load ownerClan
            String ownerClanName = db.getString("outposts." + id + ".ownerClan");
            Clan ownerClan = null;
            if (ownerClanName != null && !ownerClanName.isEmpty()) {
                ownerClan = ClanUtils.getClanByName(ownerClanName);
            }

            // Load rewarded players
            List<String> rewardedPlayersList = db.getStringList("outposts." + id + ".rewardedPlayers");
            Set<UUID> rewardedPlayers = new HashSet<>();
            for (String uuidStr : rewardedPlayersList) {
                rewardedPlayers.add(UUID.fromString(uuidStr));
            }

            // Create the Outpost object
            Outpost outpost = new Outpost(id, displayName, corner1, corner2, claimTime, oneTimeRewards, recurringRewards);
            outpost.setProgress(progress);
            outpost.setOwnerClan(ownerClan);
            outpost.setRewardedPlayers(rewardedPlayers);

            // Load additional state if needed
            outpost.loadState();
            outpost.updateHologram();

            addOutpost(id, outpost);
        }
    }

    public static void saveOutpostConfig(Outpost outpost) {
        FileConfiguration config = Main.getPlugin().getConfig();
        String path = "outposts." + outpost.getId();

        // Save display name
        config.set(path + ".name", outpost.getDisplayName());

        // Save claim time
        config.set(path + ".claim_time", outpost.getClaimTime());

        // Save rewards
        String rewardsPath = path + ".rewards";
        config.set(rewardsPath, null); // Clear existing rewards

        // Save one-time rewards
        String oneTimePath = rewardsPath + ".one-time";
        int index = 1;
        for (Reward reward : outpost.getOneTimeRewards()) {
            String rewardPath = oneTimePath + "." + index;
            saveRewardToConfig(config, rewardPath, reward);
            index++;
        }

        // Save recurring rewards
        String recurringPath = rewardsPath + ".recurring";
        index = 1;
        for (Reward reward : outpost.getRecurringRewards()) {
            String rewardPath = recurringPath + "." + index;
            saveRewardToConfig(config, rewardPath, reward);
            index++;
        }

        Main.getPlugin().saveConfig();
    }

    private static void saveRewardToConfig(FileConfiguration config, String rewardPath, Reward reward) {
        config.set(rewardPath + ".type", reward.getType());
        if (reward.getPercent() != null) {
            config.set(rewardPath + ".percent", reward.getPercent());
        }
        if (reward.getDisplay() != null) {
            config.set(rewardPath + ".display", reward.getDisplay());
        }
        if (reward.getCommand() != null) {
            config.set(rewardPath + ".command", reward.getCommand());
        }
        if (reward.isRecurring() && reward.getInterval() != null) {
            config.set(rewardPath + ".interval", reward.getInterval());
        }
    }


    public static void saveOutposts() {
        for (Outpost outpost : outposts.values()) {
            outpost.saveState();
        }
    }

    public static void addOutpost(String name, Outpost outpost) {
        outposts.put(name, outpost);
    }

    public static Outpost getOutpost(String name) {
        return outposts.get(name);
    }

    public static Map<String, Outpost> getOutposts() {
        return outposts;
    }

    public static void reloadOutposts() {
        // Unload existing outposts
        for (Outpost outpost : outposts.values()) {
            outpost.unload();
        }
        outposts.clear();

        // Reload the outposts from the updated configuration
        loadOutposts();
    }

    public static String normalize(String input) {
        if (input == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input))
                .replaceAll("\\s+", " ") // Replace multiple spaces with a single space
                .trim()
                .toLowerCase(); // Convert to lowercase for case-insensitive comparison
    }

    public static Outpost getOutpostByDisplayName(String displayName) {
        displayName = normalize(displayName);
        for (Outpost outpost : outposts.values()) {
            String outpostDisplayName = normalize(outpost.getDisplayName());
            if (outpostDisplayName.equals(displayName)) {
                return outpost;
            }
        }
        return null;
    }

    // Helper methods to save and load Locations
    public static void saveLocation(FileConfiguration config, String path, Location loc) {
        if (loc == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    public static Location loadLocation(FileConfiguration config, String path) {
        if (!config.contains(path + ".world")) {
            return null;
        }
        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // World is not loaded
            return null;
        }
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
