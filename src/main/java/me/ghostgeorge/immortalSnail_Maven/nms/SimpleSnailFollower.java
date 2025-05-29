package me.ghostgeorge.immortalSnail_Maven.nms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Enhanced Bukkit-only snail follower - no NMS required
 * This is more reliable across different server versions
 */
public class SimpleSnailFollower {

    public static void startFollowing(Entity snailEntity, Player target, Plugin plugin) {
        System.out.println("DEBUG: Starting enhanced simple follower for " + target.getName());

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Safety checks
            if (!snailEntity.isValid() || !target.isOnline()) {
                System.out.println("DEBUG: Stopping follower - entity invalid or player offline");
                return;
            }

            Location playerLoc = target.getLocation();
            Location snailLoc = snailEntity.getLocation();

            // Must be in same world
            if (!playerLoc.getWorld().equals(snailLoc.getWorld())) {
                return;
            }

            double distance = snailLoc.distance(playerLoc);

            // Less frequent debug output
            if (snailEntity.getTicksLived() % 40 == 0) { // Every 2 seconds
                System.out.println("DEBUG: Snail distance from " + target.getName() + ": " + String.format("%.2f", distance));
            }

            // Teleport if too far (failsafe)
            if (distance > 40) {
                System.out.println("DEBUG: Teleporting snail closer to " + target.getName());
                Location teleportLoc = playerLoc.clone().add(
                        (Math.random() - 0.5) * 16, // Random X offset (-8 to +8)
                        0,
                        (Math.random() - 0.5) * 16  // Random Z offset (-8 to +8)
                );
                teleportLoc.setY(teleportLoc.getWorld().getHighestBlockYAt(teleportLoc) + 1);
                snailEntity.teleport(teleportLoc);
                return;
            }

            // If very close, don't move (stop distance)
            if (distance < 2.0) {
                // Apply small downward velocity to make sure it stays on ground
                snailEntity.setVelocity(new Vector(0, -0.1, 0));
                return;
            }

            // Calculate direction to player
            Vector direction = playerLoc.toVector().subtract(snailLoc.toVector()).normalize();

            // Snail speed - very slow like a real snail
            double speed = 0.15;

            // Apply velocity toward player
            Vector velocity = direction.multiply(speed);

            // Add slight upward component to help with terrain and prevent getting stuck
            velocity.setY(0.05);

            // Check if snail is stuck (hasn't moved much in recent ticks)
            Vector currentVelocity = snailEntity.getVelocity();
            if (currentVelocity.length() < 0.01 && distance > 3.0) {
                // Snail might be stuck, give it a little jump
                velocity.setY(0.3);
                System.out.println("DEBUG: Snail might be stuck, adding jump velocity");
            }

            // Apply the velocity
            snailEntity.setVelocity(velocity);

            // Make sure the snail has gravity and physics
            snailEntity.setGravity(true);

        }, 0L, 8L); // Run every 8 ticks (0.4 seconds) for smoother movement
    }
}