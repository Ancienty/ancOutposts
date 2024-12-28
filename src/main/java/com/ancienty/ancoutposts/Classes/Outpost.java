package com.ancienty.ancoutposts.Classes;

import com.ancienty.ancoutposts.Main;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import com.ancienty.ancoutposts.Managers.RewardManager;
import com.ancienty.ancoutposts.Utils.ClanUtils;
import dev.risas.neron.models.clan.Clan;
import dev.risas.neron.models.clan.member.ClanMember;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Outpost {

    private String id;
    private String displayName;
    private Location corner1;
    private Location corner2;
    private BukkitTask progressTask;
    private int claimTime;
    private Map<UUID, Clan> playersInOutpost = new HashMap<>();
    private double progress = 0;
    private Clan progressClan;
    private Clan ownerClan;

    private Map<UUID, PendingPercentageRewards> pendingCumulativePercentageRewards = new HashMap<>();
    private Map<UUID, List<String>> pendingCommandRewards = new HashMap<>();

    private List<Reward> oneTimeRewards;
    private List<Reward> recurringRewards;
    private List<BukkitTask> recurringRewardTasks = new ArrayList<>();

    private Set<UUID> rewardedPlayers = new HashSet<>();

    private Hologram hologram;

    public Outpost(String id, String displayName, Location corner1, Location corner2, int claimTime, List<Reward> oneTimeRewards, List<Reward> recurringRewards) {
        this.id = id;
        this.displayName = displayName;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.claimTime = claimTime;
        this.oneTimeRewards = oneTimeRewards;
        this.recurringRewards = recurringRewards;

        initializeHologram();
    }

    /**
     * Sets the owner clan of the outpost.
     *
     * @param ownerClan The clan that owns the outpost.
     */
    public void setOwnerClan(Clan ownerClan) {
        this.ownerClan = ownerClan;
    }

    /**
     * Sets the current progress of the outpost claim.
     *
     * @param progress The progress value (0 to 100).
     */
    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * Sets the set of players who have received rewards from this outpost.
     *
     * @param rewardedPlayers The set of player UUIDs.
     */
    public void setRewardedPlayers(Set<UUID> rewardedPlayers) {
        this.rewardedPlayers = rewardedPlayers;
    }

    /**
     * Initializes the hologram at the saved location or at the center if none exists.
     */
    private void initializeHologram() {
        Location hologramLocation = getHologramLocation();
        if (hologramLocation == null) {
            hologramLocation = getCenterLocation().add(0, 2, 0);
            saveHologramLocation(hologramLocation);
        }
        String hologramName = "outpost_" + id;

        hologram = DHAPI.getHologram(hologramName);

        if (hologram != null) {
            hologram.setLocation(hologramLocation);
        } else {
            hologram = DHAPI.createHologram(hologramName, hologramLocation);
        }

        updateHologram();
    }

    /**
     * Gets the hologram location from the config.yml or returns null if not set.
     *
     * @return The hologram location.
     */
    private Location getHologramLocation() {
        FileConfiguration config = Main.getPlugin().getConfig();
        String path = "outposts." + id + ".hologramLocation";
        if (config.contains(path)) {
            return deserializeLocation(config.getConfigurationSection(path));
        } else {
            return null;
        }
    }

    /**
     * Saves the hologram location to the config.yml file.
     *
     * @param location The location to save.
     */
    private void saveHologramLocation(Location location) {
        FileConfiguration config = Main.getPlugin().getConfig();
        String path = "outposts." + id + ".hologramLocation";
        serializeLocation(config.createSection(path), location);
        Main.getPlugin().saveConfig();
    }

    private void serializeLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private Location deserializeLocation(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    /**
     * Updates the hologram based on the outpost's state.
     */
    public void updateHologram() {
        FileConfiguration config = Main.getPlugin().getConfig();
        List<String> lines;

        if (progressClan != null && progress > 0 && progress < 100) {
            lines = config.getStringList("hologram.being_claimed");
        } else if (ownerClan != null) {
            lines = config.getStringList("hologram.claimed");
        } else {
            lines = config.getStringList("hologram.unclaimed");
        }

        List<String> parsedLines = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("{rewards}")) {
                List<String> rewardLines = getRewardLines();
                for (String rewardLine : rewardLines) {
                    parsedLines.add(ChatColor.translateAlternateColorCodes('&', rewardLine));
                }
            } else {
                parsedLines.add(ChatColor.translateAlternateColorCodes('&', parsePlaceholders(line)));
            }
        }

        if (parsedLines.isEmpty()) {
            parsedLines.add(ChatColor.translateAlternateColorCodes('&', "&7No information available."));
        }

        DHAPI.setHologramLines(hologram, parsedLines);
    }

    /**
     * Parses placeholders in hologram lines.
     *
     * @param line The line to parse.
     * @return The parsed line.
     */
    private String parsePlaceholders(String line) {
        line = line.replace("{outpost_name}", displayName);

        String clanName = ownerClan != null ? ownerClan.getName() : "None";
        line = line.replace("{clan_name}", clanName);

        String progressClanName = progressClan != null ? progressClan.getName() : "None";
        line = line.replace("{progress_clan_name}", progressClanName);

        line = line.replace("{progress}", String.format("%.1f%%", progress));

        return line;
    }

    /**
     * Gets the reward lines for holograms or lores.
     *
     * @return List of reward lines.
     */
    private List<String> getRewardLines() {
        List<String> rewardLines = new ArrayList<>();
        for (Reward reward : oneTimeRewards) {
            String rewardDisplay = reward.getDisplay();
            if (rewardDisplay != null) {
                rewardLines.add(ChatColor.translateAlternateColorCodes('&', rewardDisplay));
            } else {
                String rewardLine = "&8- &7" + reward.getType();
                if (reward.getPercent() != null) {
                    rewardLine += " " + reward.getPercent() + "%";
                }
                rewardLines.add(ChatColor.translateAlternateColorCodes('&', rewardLine));
            }
        }
        for (Reward reward : recurringRewards) {
            String rewardDisplay = reward.getDisplay();
            if (rewardDisplay != null) {
                rewardLines.add(ChatColor.translateAlternateColorCodes('&', rewardDisplay + " (Every " + reward.getInterval() + " min)"));
            } else {
                String rewardLine = "&8- &7" + reward.getType() + " (Every " + reward.getInterval() + " min)";
                if (reward.getPercent() != null) {
                    rewardLine += " " + reward.getPercent() + "%";
                }
                rewardLines.add(ChatColor.translateAlternateColorCodes('&', rewardLine));
            }
        }
        return rewardLines;
    }

    /**
     * Gets the center location of the outpost area.
     *
     * @return The center location.
     */
    private Location getCenterLocation() {
        double centerX = (corner1.getX() + corner2.getX()) / 2;
        double centerY = (corner1.getY() + corner2.getY()) / 2;
        double centerZ = (corner1.getZ() + corner2.getZ()) / 2;
        return new Location(corner1.getWorld(), centerX, centerY, centerZ);
    }

    /**
     * Checks if a location is inside the outpost area.
     *
     * @param loc The location to check.
     * @return True if inside, false otherwise.
     */
    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(corner1.getWorld())) return false;

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    /**
     * Handles when a player enters the outpost area.
     *
     * @param player The player who entered.
     */
    public void playerEntered(Player player) {
        Clan playerClan = ClanUtils.getClan(player);
        playersInOutpost.put(player.getUniqueId(), playerClan);

        // Send message to the player
        Main.getPlugin().sendMessage(player, "entered_outpost", "outpost_name", displayName);

        if (!playersInOutpost.isEmpty()) {
            Clan firstClan = null;
            boolean sameClan = true;
            for (Clan clan : playersInOutpost.values()) {
                if (clan == null) {
                    sameClan = false;
                    break;
                }
                if (firstClan == null) {
                    firstClan = clan;
                } else if (!firstClan.equals(clan)) {
                    sameClan = false;
                    break;
                }
            }

            if (sameClan && firstClan != null) {
                if (ownerClan != null && ownerClan.equals(firstClan)) {
                    // Clan already owns the outpost
                    broadcastToOutpostPlayers(Main.getPlugin(), "you_already_own_outpost", "outpost_name", displayName);
                    stopProgressTask();
                    progress = 0;
                    progressClan = null;
                    updateHologram();
                } else {
                    if (progressClan == null || progressClan.equals(firstClan)) {
                        // Start or continue progress for this clan
                        progressClan = firstClan;
                        startProgressTask(false);
                        broadcastToOutpostPlayers(Main.getPlugin(), "clan_members_detected");
                    } else {
                        // Different clan; start decaying current progress
                        startProgressTask(true);
                        broadcastToOutpostPlayers(Main.getPlugin(), "players_different_clans");
                    }
                }
            } else {
                // Players from different clans; start decaying current progress
                startProgressTask(true);
                broadcastToOutpostPlayers(Main.getPlugin(), "players_different_clans");
            }
        }
    }

    /**
     * Handles when a player leaves the outpost area.
     *
     * @param player The player who left.
     */
    public void playerLeft(Player player) {
        playersInOutpost.remove(player.getUniqueId());

        // Send message to the player
        Main.getPlugin().sendMessage(player, "left_outpost", "outpost_name", displayName);

        if (playersInOutpost.isEmpty()) {
            stopProgressTask();
        } else {
            Clan firstClan = null;
            boolean sameClan = true;
            for (Clan clan : playersInOutpost.values()) {
                if (clan == null) {
                    sameClan = false;
                    break;
                }
                if (firstClan == null) {
                    firstClan = clan;
                } else if (!firstClan.equals(clan)) {
                    sameClan = false;
                    break;
                }
            }

            if (sameClan && firstClan != null) {
                if (ownerClan != null && ownerClan.equals(firstClan)) {
                    stopProgressTask();
                    progress = 0;
                    progressClan = null;
                    updateHologram();
                } else if (progressClan == null || progressClan.equals(firstClan)) {
                    progressClan = firstClan;
                    startProgressTask(false);
                } else {
                    startProgressTask(true);
                    broadcastToOutpostPlayers(Main.getPlugin(), "players_different_clans");
                }
            } else {
                startProgressTask(true);
                broadcastToOutpostPlayers(Main.getPlugin(), "players_different_clans");
            }
        }
    }

    /**
     * Starts the progress task.
     *
     * @param isDecaying True if progress should decay, false if it should increase.
     */
    private void startProgressTask(boolean isDecaying) {
        stopProgressTask();

        double progressPerSecond = (100.0 / claimTime);

        progressTask = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
            if (playersInOutpost.isEmpty()) {
                stopProgressTask();
                return;
            }

            Clan firstClan = null;
            boolean sameClan = true;
            for (Clan clan : playersInOutpost.values()) {
                if (clan == null) {
                    sameClan = false;
                    break;
                }
                if (firstClan == null) {
                    firstClan = clan;
                } else if (!firstClan.equals(clan)) {
                    sameClan = false;
                    break;
                }
            }

            if (isDecaying) {
                progress -= progressPerSecond;
                if (progress <= 0) {
                    progress = 0;
                    progressClan = null;
                    stopProgressTask();
                    updateHologram();

                    if (sameClan && firstClan != null && ownerClan == null) {
                        progressClan = firstClan;
                        startProgressTask(false);
                        broadcastToOutpostPlayers(Main.getPlugin(), "clan_members_detected");
                    }
                } else {
                    updateHologram();
                }
            } else {
                if (!sameClan || !firstClan.equals(progressClan)) {
                    startProgressTask(true);
                    broadcastToOutpostPlayers(Main.getPlugin(), "players_different_clans");
                    return;
                }

                if (ownerClan != null && ownerClan.equals(firstClan)) {
                    stopProgressTask();
                    progress = 0;
                    progressClan = null;
                    updateHologram();
                    return;
                }

                progress += progressPerSecond;
                if (progress >= 100) {
                    progress = 100;
                    claimOutpost();
                    stopProgressTask();
                }
                updateHologram();
            }
        }, 20L, 20L);
    }

    /**
     * Stops any ongoing progress task.
     */
    private void stopProgressTask() {
        if (progressTask != null) {
            progressTask.cancel();
            progressTask = null;
        }
    }

    /**
     * Claims the outpost for the clan when progress reaches 100%.
     */
    private void claimOutpost() {
        ownerClan = progressClan;
        progress = 0;
        progressClan = null;

        applyOneTimeRewardsToClanMembers();
        startRecurringRewards();

        OutpostManager.addClaimedOutpost(ownerClan, this);
        applyCumulativeRewardsToClanMembers(ownerClan);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                Main.getPlugin().getConfig().getString("lang.prefix", "") +
                        Main.getPlugin().getConfig().getString("lang.clan_claimed_outpost", "")
                                .replace("{clan_name}", ownerClan.getName())
                                .replace("{outpost_name}", displayName)
        ));
        updateHologram();
        saveState();
    }

    private void applyOneTimeRewardsToClanMembers() {
        List<String> commands = new ArrayList<>();
        for (Reward reward : oneTimeRewards) {
            if ("COMMAND".equalsIgnoreCase(reward.getType()) && reward.getCommand() != null) {
                commands.add(reward.getCommand());
            }
        }

        for (ClanMember clanMember : ownerClan.getMembers()) {
            UUID memberUUID = clanMember.getUuid();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
            if (offlinePlayer.isOnline()) {
                Player member = offlinePlayer.getPlayer();
                executeCommands(member, commands);
            } else {
                pendingCommandRewards.computeIfAbsent(memberUUID, k -> new ArrayList<>()).addAll(commands);
            }
        }
    }

    private void startRecurringRewards() {
        for (Reward reward : recurringRewards) {
            Integer interval = reward.getInterval();
            if (interval != null && interval > 0) {
                long intervalTicks = interval * 60 * 20L;
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
                    applyRecurringRewardToClanMembers(reward);
                }, intervalTicks, intervalTicks);
                recurringRewardTasks.add(task);
            }
        }
    }

    private void applyRecurringRewardToClanMembers(Reward reward) {
        if ("COMMAND".equalsIgnoreCase(reward.getType()) && reward.getCommand() != null) {
            String command = reward.getCommand();
            for (ClanMember clanMember : ownerClan.getMembers()) {
                UUID memberUUID = clanMember.getUuid();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
                if (offlinePlayer.isOnline()) {
                    Player member = offlinePlayer.getPlayer();
                    String parsedCommand = command.replace("{player}", member.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
                } else {
                    pendingCommandRewards.computeIfAbsent(memberUUID, k -> new ArrayList<>()).add(command);
                }
            }
        }
    }

    private void stopRecurringRewards() {
        for (BukkitTask task : recurringRewardTasks) {
            if (task != null) {
                task.cancel();
            }
        }
        recurringRewardTasks.clear();
    }

    /**
     * Executes a list of commands for a player.
     *
     * @param player   The player for whom to execute commands.
     * @param commands The list of commands to execute.
     */
    private void executeCommands(Player player, List<String> commands) {
        for (String command : commands) {
            if (command != null) {
                String parsedCommand = command.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
            }
        }
    }

    /**
     * Removes ownership and rewards when the outpost is unclaimed.
     */
    private void removeOwnership() {
        if (ownerClan != null) {
            OutpostManager.removeClaimedOutpost(ownerClan, this);
            applyCumulativeRewardsToClanMembers(ownerClan);
            stopRecurringRewards();

            for (ClanMember clanMember : ownerClan.getMembers()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(clanMember.getUuid());
                if (offlinePlayer.isOnline()) {
                    Player member = offlinePlayer.getPlayer();
                    if (member != null) {
                        Main.getPlugin().sendMessage(member, "clan_lost_outpost", "outpost_name", displayName);
                    }
                }
            }

            ownerClan = null;
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    Main.getPlugin().getConfig().getString("lang.prefix", "") +
                            Main.getPlugin().getConfig().getString("lang.outpost_unclaimed", "")
                                    .replace("{outpost_name}", displayName)
            ));
            updateHologram();
            saveState();
        }
    }

    /**
     * Recalculates and applies cumulative percentage rewards to all members of a clan.
     *
     * @param clan The clan whose members will receive rewards.
     */
    private void applyCumulativeRewardsToClanMembers(Clan clan) {
        // Implement any cumulative percentage rewards logic if needed
    }

    /**
     * Applies pending cumulative percentage rewards and executes pending commands for a player when they log in.
     *
     * @param player The player who logged in.
     */
    public void applyPendingRewards(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Apply pending percentage rewards
        PendingPercentageRewards pendingRewards = pendingCumulativePercentageRewards.get(playerUUID);
        if (pendingRewards != null) {
            // Apply rewards logic here if necessary
            pendingCumulativePercentageRewards.remove(playerUUID);
        }

        // Execute pending commands
        List<String> commands = pendingCommandRewards.get(playerUUID);
        if (commands != null) {
            executeCommands(player, commands);
            pendingCommandRewards.remove(playerUUID);
        }
    }

    /**
     * Broadcasts a message to all players currently in the outpost.
     *
     * @param plugin    The main plugin instance.
     * @param langKey   The language key for the message.
     * @param variables Variables to replace in the message.
     */
    private void broadcastToOutpostPlayers(Main plugin, String langKey, String... variables) {
        for (UUID playerId : playersInOutpost.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.sendMessage(player, langKey, variables);
            }
        }
    }

    /**
     * Resets the ownership of the outpost.
     */
    public void resetOwnership() {
        removeOwnership();
        progress = 0;
        progressClan = null;
        updateHologram();
        saveState();
    }

    /**
     * Teleports the hologram to a new location.
     *
     * @param location The new location.
     */
    public void teleportHologram(Location location) {
        if (hologram != null) {
            hologram.setLocation(location);
        } else {
            initializeHologram();
            hologram.setLocation(location);
        }
        updateHologram();
        saveHologramLocation(location);
    }

    /**
     * Saves the outpost's dynamic state to the database.yml file.
     */
    public void saveState() {
        FileConfiguration db = Main.getPlugin().getDatabaseConfig();

        db.set("outposts." + id + ".ownerClan", ownerClan != null ? ownerClan.getName() : null);
        db.set("outposts." + id + ".progress", progress);

        List<String> rewardedPlayersList = new ArrayList<>();
        for (UUID uuid : rewardedPlayers) {
            rewardedPlayersList.add(uuid.toString());
        }
        db.set("outposts." + id + ".rewardedPlayers", rewardedPlayersList);

        Main.getPlugin().saveDatabase();
    }

    /**
     * Loads the outpost's dynamic state from the database.yml file.
     */
    public void loadState() {
        FileConfiguration db = Main.getPlugin().getDatabaseConfig();

        String ownerClanName = db.getString("outposts." + id + ".ownerClan");
        if (ownerClanName != null) {
            ownerClan = ClanUtils.getClanByName(ownerClanName);
        }

        progress = db.getDouble("outposts." + id + ".progress", 0);

        rewardedPlayers.clear();
        List<String> rewardedPlayersList = db.getStringList("outposts." + id + ".rewardedPlayers");
        for (String uuidStr : rewardedPlayersList) {
            rewardedPlayers.add(UUID.fromString(uuidStr));
        }

        initializeHologram();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    public int getClaimTime() {
        return claimTime;
    }

    public void setClaimTime(int claimTime) {
        this.claimTime = claimTime;
    }

    public List<Reward> getOneTimeRewards() {
        return oneTimeRewards;
    }

    public List<Reward> getRecurringRewards() {
        return recurringRewards;
    }

    public double getProgress() {
        return progress;
    }

    public Clan getProgressClan() {
        return progressClan;
    }

    public Clan getOwnerClan() {
        return ownerClan;
    }

    public String getOwnerClanName() {
        return ownerClan != null ? ownerClan.getName() : "None";
    }

    public void unload() {
        stopProgressTask();

        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }

        playersInOutpost.clear();
    }
}
