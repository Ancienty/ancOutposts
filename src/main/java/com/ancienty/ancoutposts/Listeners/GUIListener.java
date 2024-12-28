package com.ancienty.ancoutposts.Listeners;

import com.ancienty.ancoutposts.GUIs.AdminGUI;
import com.ancienty.ancoutposts.GUIs.OutpostEditorGUI;
import com.ancienty.ancoutposts.GUIs.OutpostGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(AdminGUI.ADMIN_GUI_TITLE)) {
            event.setCancelled(true);

            // Check if the clicked inventory is the top inventory (your GUI)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
                AdminGUI.handleAdminGUIClick(player, event.getSlot(), event.getClick());
            }
        } else if (inventoryTitle.equals(OutpostEditorGUI.EDITOR_GUI_TITLE) || inventoryTitle.equals(OutpostEditorGUI.EDIT_REWARDS_GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
                OutpostEditorGUI.handleEditorGUIClick(player, event.getSlot(), event.getClick());
            }
        } else if (inventoryTitle.equals(OutpostGUI.OUTPOST_GUI_TITLE)) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
                OutpostGUI.handleOutpostGUIClick(event);
            }
        }
    }
}
