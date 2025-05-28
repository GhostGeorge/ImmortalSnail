package me.ghostgeorge.immortalSnail_Maven.nms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Simple Bukkit-only snail follower - no NMS required
 * This is more reliable across different server versions
 */
public class SimpleSnailFollower {

    public static void startFollowing(Entity snailEntity, Player target, Plugin plugin) {
        System.out.println("DEBUG: Starting simple follower for " + target.getName());

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Safety checks
            if (!snailEntity.isValid() || !target.isOnline()) {
                System.out.println("DEBUG: Stopping follower - entity invalid or player offline");
                return;
            }

            // Check if snail game is still active (you'll need to pass this or check it)
            // For now, assume it's always active

            Location playerLoc = target.getLocation();
            Location snailLoc = snailEntity.getLocation();

            // Must be in same world
            if (!playerLoc.getWorld().equals(snailLoc.getWorld())) {
                return;
            }

            double distance = snailLoc.distance(playerLoc);

            System.out.println("DEBUG: Snail distance from " + target.getName() + ": " + String.format("%.2f", distance));

            // Teleport if too far (failsafe)
            if (distance > 50) {
                System.out.println("DEBUG: Teleporting snail closer to " + target.getName());
                Location teleportLoc = playerLoc.clone().add(
                        (Math.random() - 0.5) * 20, // Random X offset
                        0,
                        (Math.random() - 0.5) * 20  // Random Z offset
                );
                teleportLoc.setY(teleportLoc.getWorld().getHighestBlockYAt(teleportLoc) + 1);
                snailEntity.teleport(teleportLoc);
                return;
            }

            // If close enough, don't move
            if (distance < 2.0) {
                return;
            }

            // Calculate direction to player
            Vector direction = playerLoc.toVector().subtract(snailLoc.toVector()).normalize();

            // Slow snail speed
            double speed = 0.1; // Very slow like a real snail

            // Apply velocity toward player
            Vector velocity = direction.multiply(speed);

            // Add slight upward component to help with terrain
            velocity.setY(0.1);

            // Apply the velocity
            snailEntity.setVelocity(velocity);

            System.out.println("DEBUG: Applied velocity to snail: " + velocity);

        }, 0L, 10L); // Run every 10 ticks (0.5 seconds)
    }
}