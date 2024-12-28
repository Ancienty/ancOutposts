package com.ancienty.ancoutposts.Managers;

import com.ancienty.ancoutposts.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager class to handle rewards related to increased and reduced damage.
 */
public class RewardManager implements Listener {

    private static Map<UUID, Integer> increasedDamagePlayers = new HashMap<>();
    private static Map<UUID, Integer> reducedDamagePlayers = new HashMap<>();

    /**
     * Adds a player to the increased damage list.
     *
     * @param playerUUID The UUID of the player.
     * @param percent    The percentage of increased damage.
     */
    public static void addIncreasedDamagePlayer(UUID playerUUID, int percent) {
        increasedDamagePlayers.put(playerUUID, percent);
    }

    /**
     * Adds a player to the reduced damage list.
     *
     * @param playerUUID The UUID of the player.
     * @param percent    The percentage of reduced damage.
     */
    public static void addReducedDamagePlayer(UUID playerUUID, int percent) {
        reducedDamagePlayers.put(playerUUID, percent);
    }

    /**
     * Removes a player from both the increased and reduced damage lists.
     *
     * @param playerUUID The UUID of the player.
     */
    public static void removePlayer(UUID playerUUID) {
        increasedDamagePlayers.remove(playerUUID);
        reducedDamagePlayers.remove(playerUUID);
    }

    /**
     * Saves the rewards to the database.yml file.
     */
    public static void saveRewardsToDatabase() {
        FileConfiguration db = Main.getPlugin().getDatabaseConfig();

        // Save increased damage players
        ConfigurationSection increasedSection = db.createSection("rewards.increasedDamagePlayers");
        for (Map.Entry<UUID, Integer> entry : increasedDamagePlayers.entrySet()) {
            increasedSection.set(entry.getKey().toString(), entry.getValue());
        }

        // Save reduced damage players
        ConfigurationSection reducedSection = db.createSection("rewards.reducedDamagePlayers");
        for (Map.Entry<UUID, Integer> entry : reducedDamagePlayers.entrySet()) {
            reducedSection.set(entry.getKey().toString(), entry.getValue());
        }

        Main.getPlugin().saveDatabase();
    }

    /**
     * Loads the rewards from the database.yml file.
     */
    public static void loadRewardsFromDatabase() {
        FileConfiguration db = Main.getPlugin().getDatabaseConfig();

        // Load increased damage players
        increasedDamagePlayers.clear();
        if (db.contains("rewards.increasedDamagePlayers")) {
            ConfigurationSection increasedSection = db.getConfigurationSection("rewards.increasedDamagePlayers");
            for (String key : increasedSection.getKeys(false)) {
                UUID playerUUID = UUID.fromString(key);
                int percent = increasedSection.getInt(key);
                increasedDamagePlayers.put(playerUUID, percent);
            }
        }

        // Load reduced damage players
        reducedDamagePlayers.clear();
        if (db.contains("rewards.reducedDamagePlayers")) {
            ConfigurationSection reducedSection = db.getConfigurationSection("rewards.reducedDamagePlayers");
            for (String key : reducedSection.getKeys(false)) {
                UUID playerUUID = UUID.fromString(key);
                int percent = reducedSection.getInt(key);
                reducedDamagePlayers.put(playerUUID, percent);
            }
        }
    }

    /**
     * Event handler for entity damage events.
     *
     * @param event The EntityDamageByEntityEvent.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity victimEntity = event.getEntity();

        // Handle increased damage for damager
        if (damagerEntity instanceof Player) {
            Player damager = (Player) damagerEntity;
            UUID damagerUUID = damager.getUniqueId();
            int increasedDamagePercent = getIncreasedDamagePercent(damagerUUID);
            if (increasedDamagePercent > 0) {
                double damage = event.getDamage();
                double newDamage = damage + (damage * increasedDamagePercent / 100.0);
                event.setDamage(newDamage);
            }
        }

        // Handle reduced damage for victim
        if (victimEntity instanceof Player) {
            Player victim = (Player) victimEntity;
            UUID victimUUID = victim.getUniqueId();
            int reducedDamagePercent = getReducedDamagePercent(victimUUID);
            if (reducedDamagePercent > 0) {
                double damage = event.getDamage();
                double newDamage = damage - (damage * reducedDamagePercent / 100.0);
                // Ensure damage does not go below zero
                if (newDamage < 0) {
                    newDamage = 0;
                }
                event.setDamage(newDamage);
            }
        }
    }

    /**
     * Gets the increased damage percentage for a player.
     *
     * @param playerUUID The UUID of the player.
     * @return The increased damage percentage, or 0 if none.
     */
    public static int getIncreasedDamagePercent(UUID playerUUID) {
        return increasedDamagePlayers.getOrDefault(playerUUID, 0);
    }

    /**
     * Gets the reduced damage percentage for a player.
     *
     * @param playerUUID The UUID of the player.
     * @return The reduced damage percentage, or 0 if none.
     */
    public static int getReducedDamagePercent(UUID playerUUID) {
        return reducedDamagePlayers.getOrDefault(playerUUID, 0);
    }
}
