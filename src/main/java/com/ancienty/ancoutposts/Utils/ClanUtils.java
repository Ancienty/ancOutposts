package com.ancienty.ancoutposts.Utils;

import dev.risas.neron.Neron;
import dev.risas.neron.models.clan.Clan;
import dev.risas.neron.models.clan.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ClanUtils {

    private static Neron neron;

    static {
        try {
            Neron neronPlugin = JavaPlugin.getPlugin(Neron.class);
            if (neronPlugin != null) {
                neron = Neron.getPlugin();
            } else {
                neron = null;
                Bukkit.getLogger().warning("NeronPlugin is not loaded. Clan functionalities will be disabled.");
            }
        } catch (Exception e) {
            neron = null;
            Bukkit.getLogger().severe("An error occurred while initializing ClanUtils:");
            e.printStackTrace();
        }
    }

    /**
     * Gets the clan of a player.
     *
     * @param player The player.
     * @return The clan, or null if the player is not in a clan.
     */
    public static Clan getClan(Player player) {
        ClanManager clanManager = neron.getClanManager();
        return clanManager.getClanByMember(player);
    }

    /**
     * Gets the clan of a player by UUID (supports offline players).
     *
     * @param playerUUID The player's UUID.
     * @return The clan, or null if the player is not in a clan.
     */
    public static Clan getClan(UUID playerUUID) {
        ClanManager clanManager = neron.getClanManager();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
            return clanManager.getClanByMember(offlinePlayer.getName());
        }
        return null;
    }

    /**
     * Gets a clan by its name.
     *
     * @param clanName The name of the clan.
     * @return The clan, or null if not found.
     */
    public static Clan getClanByName(String clanName) {
        ClanManager clanManager = neron.getClanManager();
        return clanManager.getClan(clanName);
    }
}
