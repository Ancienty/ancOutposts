package com.ancienty.ancoutposts.Listeners;

import com.ancienty.ancoutposts.Classes.Outpost;
import com.ancienty.ancoutposts.Managers.OutpostManager;
import com.ancienty.ancoutposts.Managers.RewardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

public class OutpostListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (Outpost outpost : OutpostManager.getOutposts().values()) {
            boolean wasInside = outpost.isInside(event.getFrom());
            boolean isInside = outpost.isInside(event.getTo());

            if (!wasInside && isInside) {
                outpost.playerEntered(player);
            } else if (wasInside && !isInside) {
                outpost.playerLeft(player);
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        for (Outpost outpost : OutpostManager.getOutposts().values()) {
            boolean wasInside = outpost.isInside(event.getFrom());
            boolean isInside = outpost.isInside(event.getTo());

            if (!wasInside && isInside) {
                outpost.playerEntered(player);
            } else if (wasInside && !isInside) {
                outpost.playerLeft(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (Outpost outpost : OutpostManager.getOutposts().values()) {
            if (outpost.isInside(player.getLocation())) {
                outpost.playerLeft(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Outpost outpost : OutpostManager.getOutposts().values()) {
            outpost.applyPendingRewards(player);
            // Check if the player is inside any outpost upon joining
            if (outpost.isInside(player.getLocation())) {
                outpost.playerEntered(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        int increasedDamagePercent = RewardManager.getIncreasedDamagePercent(damager.getUniqueId());
        if (increasedDamagePercent > 0) {
            double newDamage = event.getDamage() * (1 + (increasedDamagePercent / 100.0));
            event.setDamage(newDamage);
        }

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            int reducedDamagePercent = RewardManager.getReducedDamagePercent(victim.getUniqueId());
            if (reducedDamagePercent > 0) {
                double newDamage = event.getDamage() * (1 - (reducedDamagePercent / 100.0));
                event.setDamage(newDamage);
            }
        }
    }
}
