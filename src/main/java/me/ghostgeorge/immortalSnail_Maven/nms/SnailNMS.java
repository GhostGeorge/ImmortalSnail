package me.ghostgeorge.immortalSnail_Maven.nms;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Pig;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.EnumSet;

/**
 * Handles NMS pathfinding for the immortal snail - Simplified for 1.21.4
 */
public class SnailNMS {

    /**
     * Adds pathfinding behavior to a pig entity to make it follow a specific player
     * @param hostEntity The pig entity hosting the snail model
     * @param target The player this snail should follow
     * @return True if pathfinding was added successfully
     */
    public static boolean addPathfindingToEntity(Entity hostEntity, Player target) {
        try {
            if (!(hostEntity instanceof org.bukkit.entity.Pig bukkitPig)) {
                System.out.println("DEBUG: Host entity is not a pig");
                return false;
            }

            // Get NMS entity
            net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) hostEntity).getHandle();

            if (!(nmsEntity instanceof Pig nmsPig)) {
                System.out.println("DEBUG: NMS entity is not a pig");
                return false;
            }

            System.out.println("DEBUG: Successfully got NMS pig for " + target.getName());

            // Clear existing AI goals
            nmsPig.goalSelector.removeAllGoals(goal -> true);
            nmsPig.targetSelector.removeAllGoals(goal -> true);

            // Set movement speed at the NMS level
            try {
                nmsPig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                        .setBaseValue(0.5D);
            } catch (Exception e) {
                System.out.println("DEBUG: Could not set movement speed: " + e.getMessage());
            }

            // Add our custom pathfinding goal with highest priority
            nmsPig.goalSelector.addGoal(1, new SimpleSnailFollowGoal(
                    nmsPig,
                    ((CraftPlayer) target).getHandle(),
                    0.5D,   // Movement speed
                    2.0F,   // Stop distance (blocks)
                    25.0F   // Start following distance (blocks)
            ));

            System.out.println("DEBUG: Added pathfinding goal for " + target.getName());
            return true;
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in addPathfindingToEntity: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Simplified pathfinding goal that only uses basic navigation methods
     */
    private static class SimpleSnailFollowGoal extends Goal {
        private final Pig snail;
        private final net.minecraft.world.entity.player.Player owner;
        private final double speed;
        private final float stopDistance;
        private final float startDistance;
        private final PathNavigation navigation;

        private final double stopDistanceSq;
        private final double startDistanceSq;
        private int timeToRecalcPath = 0;

        public SimpleSnailFollowGoal(Pig snail, net.minecraft.world.entity.player.Player owner,
                                     double speed, float stopDistance, float startDistance) {
            this.snail = snail;
            this.owner = owner;
            this.speed = speed;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
            this.navigation = snail.getNavigation();

            this.stopDistanceSq = stopDistance * stopDistance;
            this.startDistanceSq = startDistance * startDistance;

            // Set the goal flags
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.owner == null || this.owner.isSpectator() || this.owner.isDeadOrDying()) {
                return false;
            }

            double distanceSq = this.snail.distanceToSqr(this.owner);
            return distanceSq > this.stopDistanceSq;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.owner == null || this.owner.isSpectator() || this.owner.isDeadOrDying()) {
                return false;
            }

            double distanceSq = this.snail.distanceToSqr(this.owner);
            return distanceSq > this.stopDistanceSq;
        }

        @Override
        public void start() {
            System.out.println("DEBUG: SimpleSnailFollowGoal started!");
            this.timeToRecalcPath = 0;

            // Use the simplest navigation method
            boolean success = this.navigation.moveTo(this.owner, this.speed);
            System.out.println("DEBUG: Initial navigation started: " + success);
        }

        @Override
        public void stop() {
            System.out.println("DEBUG: SimpleSnailFollowGoal stopped");
            this.navigation.stop();
        }

        @Override
        public void tick() {
            double distanceSq = this.snail.distanceToSqr(this.owner);
            double distance = Math.sqrt(distanceSq);

            // Make the snail look at the player
            this.snail.getLookControl().setLookAt(
                    this.owner,
                    10.0F,
                    this.snail.getMaxHeadXRot()
            );

            // Recalculate path every 10 ticks (0.5 seconds)
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;

                System.out.println("DEBUG: Distance: " + String.format("%.2f", distance) +
                        ", Navigation active: " + this.navigation.isInProgress());

                // Teleport if too far away
                if (distance > 40) {
                    System.out.println("DEBUG: Teleporting snail closer to player");
                    double angle = Math.random() * 2 * Math.PI;
                    double offsetX = Math.cos(angle) * 8;
                    double offsetZ = Math.sin(angle) * 8;

                    this.snail.teleportTo(
                            this.owner.getX() + offsetX,
                            this.owner.getY(),
                            this.owner.getZ() + offsetZ
                    );
                    return;
                }

                // If we're in the right range and not currently navigating, start navigation
                if (distance > this.stopDistance && distance < this.startDistance) {
                    if (!this.navigation.isInProgress()) {
                        boolean success = this.navigation.moveTo(this.owner, this.speed);
                        System.out.println("DEBUG: Restarting navigation: " + success);

                        // If that fails, try with coordinates
                        if (!success) {
                            success = this.navigation.moveTo(
                                    this.owner.getX(),
                                    this.owner.getY(),
                                    this.owner.getZ(),
                                    this.speed
                            );
                            System.out.println("DEBUG: Coordinate navigation: " + success);
                        }
                    }
                }
            }
        }
    }
}