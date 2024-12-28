package com.ancienty.ancoutposts;

import com.ancienty.ancoutposts.Classes.OutpostCreationState;
import com.ancienty.ancoutposts.Classes.RewardCreationState;
import com.ancienty.ancoutposts.Commands.OutpostCommand;
import com.ancienty.ancoutposts.Listeners.ChatListener;
import com.ancienty.ancoutposts.Listeners.GUIListener;
import com.ancienty.ancoutposts.Listeners.OutpostListener;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import com.ancienty.ancoutposts.Managers.RewardManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main plugin;
    private File databaseFile;
    private FileConfiguration databaseConfig;
    private FileConfiguration config;
    private Map<UUID, RewardCreationState> rewardCreationStates = new HashMap<>();
    private Map<UUID, OutpostCreationState> outpostCreationStates = new HashMap<>();
    private Map<UUID, Location[]> selections = new HashMap<>();

    @Override
    public void onEnable() {

        getLogger().info("Enabling plugin.");
        getLogger().info("Checking dependencies.");
        if (getServer().getPluginManager().getPlugin("Neron") == null) {
            getLogger().info("Couldn't find Neron plugin, disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Dependencies found. Reading config.yml");
        plugin = this;
        saveDefaultConfig();
        getLogger().info("Reading database.yml");
        createDatabaseFile();
        config = getConfig();

        // Load data from database.yml
        loadDatabase();

        // Register commands and events
        getLogger().info("Registering commands and listeners.");
        getCommand("outpost").setExecutor(new OutpostCommand(this));
        getServer().getPluginManager().registerEvents(new OutpostListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this); // Register GUI listener
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new RewardManager(), this);

        // Load outposts from config.yml
        getLogger().info("Loading outposts and rewards.");
        OutpostManager.loadOutposts();
        RewardManager.loadRewardsFromDatabase();
    }

    @Override
    public void onDisable() {
        // Save data to database.yml
        getLogger().info("Disabling plugin, saving important data.");
        saveDatabase();
        RewardManager.saveRewardsToDatabase();
        getLogger().info("Data saved, disabling.");
    }

    public Map<UUID, RewardCreationState> getRewardCreationStates() {
        return rewardCreationStates;
    }

    public Map<UUID, OutpostCreationState> getOutpostCreationStates() {
        return outpostCreationStates;
    }

    public Map<UUID, Location[]> getSelections() {
        return selections;
    }

    public static Main getPlugin() {
        return plugin;
    }

    private void createDatabaseFile() {
        databaseFile = new File(getDataFolder(), "database.yml");
        if (!databaseFile.exists()) {
            databaseFile.getParentFile().mkdirs();
            try {
                databaseFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        databaseConfig = new YamlConfiguration();
        try {
            databaseConfig.load(databaseFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getDatabaseConfig() {
        return this.databaseConfig;
    }

    public void saveDatabase() {
        try {
            databaseConfig.save(databaseFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDatabase() {
        try {
            databaseConfig.load(databaseFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        // Load data into RewardManager
        RewardManager.loadRewardsFromDatabase();
    }

    /**
     * Sends a message to a player using the lang section in config.yml.
     *
     * @param player    The player to send the message to.
     * @param langKey   The key in the lang section.
     * @param variables Variables to replace in the message.
     */
    public void sendMessage(Player player, String langKey, String... variables) {
        String message = config.getString("lang." + langKey, "");
        String prefix = config.getString("lang.prefix", "");
        message = prefix + message;

        // Replace variables in the message
        for (int i = 0; i < variables.length; i += 2) {
            if (i + 1 < variables.length) {
                message = message.replace("{" + variables[i] + "}", variables[i + 1]);
            }
        }

        // Translate color codes
        message = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(message);
    }
}
