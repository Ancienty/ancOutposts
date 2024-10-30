package com.ancienty.ancoutposts.Rewards;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardManager {

    private static Map<UUID, Integer> increasedDamagePlayers = new HashMap<>();
    private static Map<UUID, Integer> reducedDamagePlayers = new HashMap<>();

    public static void addIncreasedDamagePlayer(UUID playerId, int percent) {
        increasedDamagePlayers.put(playerId, percent);
    }

    public static void addReducedDamagePlayer(UUID playerId, int percent) {
        reducedDamagePlayers.put(playerId, percent);
    }

    public static int getIncreasedDamagePercent(UUID playerId) {
        return increasedDamagePlayers.getOrDefault(playerId, 0);
    }

    public static int getReducedDamagePercent(UUID playerId) {
        return reducedDamagePlayers.getOrDefault(playerId, 0);
    }

    public static void removePlayer(UUID playerId) {
        increasedDamagePlayers.remove(playerId);
        reducedDamagePlayers.remove(playerId);
    }
}
