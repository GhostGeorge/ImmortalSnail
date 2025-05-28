package me.ghostgeorge.immortalSnail_Maven.listeners;

import com.magmaguy.freeminecraftmodels.customentity.DynamicEntity;
import me.ghostgeorge.immortalSnail_Maven.ImmortalSnail_Maven;
import me.ghostgeorge.immortalSnail_Maven.nms.SnailNMS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;

public class eventlisteners implements Listener {
    private final ImmortalSnail_Maven plugin;

    public eventlisteners(ImmortalSnail_Maven plugin) {
        this.plugin = plugin;
    }

    // Makes snail invincible
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        for (DynamicEntity snail : plugin.getPlayerSnailMap().values()) {
            if (snail.getLivingEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
                event.setCancelled(true);        // make snail immortal
                return;
            }
        }
    }

    // For when a player dies
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Switch the player to spectator mode
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.sendMessage("You died and are now in spectator mode.");
        }
    }

    // Kills player if THEIR snail comes within range
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        DynamicEntity snail = plugin.getPlayerSnailMap().get(player);
        if (snail != null &&
                snail.getLivingEntity().getWorld().equals(player.getWorld()) &&
                snail.getLivingEntity().getLocation().distance(player.getLocation()) < 1.0) {

            player.setHealth(0);   // player caught by their snail
        }

        /* freeze logic */
        if (plugin.snailPaused &&
                !event.getFrom().toVector().equals(event.getTo().toVector())) {
            event.setTo(event.getFrom());
        }
    }

    // Allows snails to travel across dimensions
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.snailActive) return;

        /* remove old snail */
        DynamicEntity old = plugin.getPlayerSnailMap().remove(player);
        if (old != null) old.remove();

        /* respawn after delay */
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            Location spawnLoc = player.getLocation().clone()
                    .add(player.getLocation().getDirection().normalize().multiply(-15));
            spawnLoc.setY(player.getWorld().getHighestBlockYAt(spawnLoc) + 1);

            // Spawn new host pig
            Pig host = player.getWorld().spawn(spawnLoc, Pig.class);
            host.setInvisible(true);
            host.setSilent(true);

            // Create new dynamic entity with snail model
            DynamicEntity newSnail = DynamicEntity.create("snail", host);
            newSnail.setName(player.getName() + "'s Snail");
            newSnail.setNameVisible(true);

            // Save to map
            plugin.getPlayerSnailMap().put(player, newSnail);

            // Apply NMS pathfinding
            boolean success = SnailNMS.addPathfindingToEntity(host, player);

            if (!success) {
                // Fall back to basic following logic
                plugin.startFollowingSnail(newSnail.getLivingEntity(), player);
            }

            player.sendMessage(ChatColor.GREEN + "Your Immortal Snail followed you through dimensions!");

        }, 160L); // 8 s
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Pig pig) {
            // Check if this pig is a snail
            Player owner = null;
            for (Map.Entry<Player, DynamicEntity> entry : plugin.getPlayerSnailMap().entrySet()) {
                if (entry.getValue().getLivingEntity().getUniqueId().equals(pig.getUniqueId())) {
                    owner = entry.getKey();
                    break;
                }
            }

            if (owner == null) return; // Not a snail

            if (!(event.getTarget() instanceof Player player)) {
                event.setCancelled(true);
                return;
            }

            // Cancel if it's targeting the wrong player
            if (!player.getUniqueId().equals(owner.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}