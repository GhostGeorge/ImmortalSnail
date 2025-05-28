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
 * Handles NMS pathfinding for the immortal snail
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
            if (!(hostEntity instanceof org.bukkit.entity.Pig)) {
                return false;
            }

            // Get NMS entity
            net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) hostEntity).getHandle();

            if (!(nmsEntity instanceof Pig nmsPig)) {
                return false;
            }

            // Clear existing AI goals
            nmsPig.goalSelector.removeAllGoals(goal -> true);
            nmsPig.targetSelector.removeAllGoals(goal -> true);

            // Add our custom pathfinding goal
            nmsPig.goalSelector.addGoal(0, new SnailFollowGoal(
                    nmsPig,
                    ((CraftPlayer) target).getHandle(),
                    0.05D,  // Very slow movement speed (snail)
                    1.5F,   // Stop distance (blocks)
                    8.0F    // Start following distance (blocks)
            ));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Custom pathfinding goal for the snail to follow a specific player
     */
    private static class SnailFollowGoal extends Goal {
        private final Pig snail;
        private final net.minecraft.world.entity.player.Player owner;
        private final double speed;
        private final float stopDistance;
        private final float startDistance;
        private final PathNavigation navigation;

        private final double stopDistanceSq;
        private final double startDistanceSq;

        public SnailFollowGoal(Pig snail, net.minecraft.world.entity.player.Player owner,
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
            // Check if the owner is valid and the snail should start following
            return this.owner != null &&
                    !this.owner.isSpectator() &&
                    !this.owner.isDeadOrDying() &&
                    this.snail.distanceToSqr(this.owner) >= this.stopDistanceSq;
        }

        @Override
        public boolean canContinueToUse() {
            // Keep following as long as the owner is valid and not too close
            return !this.navigation.isDone() &&
                    this.owner != null &&
                    !this.owner.isSpectator() &&
                    !this.owner.isDeadOrDying() &&
                    this.snail.distanceToSqr(this.owner) >= this.stopDistanceSq;
        }

        @Override
        public void start() {
            // Reset follow timer
            this.navigation.moveTo(this.owner, this.speed);
        }

        @Override
        public void stop() {
            // Stop following
            this.navigation.stop();
        }

        @Override
        public void tick() {
            // Make the snail look at the player
            this.snail.getLookControl().setLookAt(
                    this.owner,
                    10.0F, // Horizontal look angle
                    this.snail.getMaxHeadXRot() // Vertical look angle
            );

            // Update navigation if needed
            if (this.snail.distanceToSqr(this.owner) >= this.startDistanceSq) {
                // If we're far from the player, keep navigating
                if (!this.navigation.isInProgress()) {
                    this.navigation.moveTo(this.owner, this.speed);
                }
            } else {
                // If we're close but not too close, just look at the player
                this.navigation.stop();
                this.snail.getLookControl().setLookAt(
                        this.owner,
                        10.0F,
                        this.snail.getMaxHeadXRot()
                );
            }
        }
    }
}
