package me.ghostgeorge.immortalSnail_Maven;

import com.magmaguy.freeminecraftmodels.customentity.DynamicEntity;
import me.ghostgeorge.immortalSnail_Maven.commands.snailcommands;
import me.ghostgeorge.immortalSnail_Maven.listeners.eventlisteners;
import me.ghostgeorge.immortalSnail_Maven.nms.SimpleSnailFollower;
import me.ghostgeorge.immortalSnail_Maven.nms.SnailNMS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public final class ImmortalSnail_Maven extends JavaPlugin {
    /*
    Known bugs
    - snail does not like to fall - override current navigation
    -  /snail pause does not work - implement a teleport freeze on players and snails
    - snail does not have a visible name - make some kind of colored marker?
    - snail can go invisible when teleporting - make a wait period after teleporting if increasing the teleport distance didnt work
    - snail can't swim - make snail float
     */

    // Determines whether the snail game is active
    public boolean snailActive = false;
    public boolean snailPaused = false;
    private final Map<Player, DynamicEntity> playerSnailMap = new HashMap<>();

    // Returns snail map to event listeners
    public Map<Player, DynamicEntity> getPlayerSnailMap() {
        return playerSnailMap;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        // Check if FreeMinecraftModels is loaded
        // Check if FreeMinecraftModels is loaded
        if (getServer().getPluginManager().getPlugin("FreeMinecraftModels") == null) {
            getLogger().severe("FreeMinecraftModels plugin not found! This plugin requires FreeMinecraftModels to work.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize FreeMinecraftModels with this plugin instance
        try {
            // Wait a tick to ensure FreeMinecraftModels is fully loaded
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    // Try to initialize FreeMinecraftModels if it has an initialization method
                    // This may vary depending on the FreeMinecraftModels version
                    getLogger().info("FreeMinecraftModels detected and ready");
                } catch (Exception e) {
                    getLogger().warning("Could not fully initialize FreeMinecraftModels: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            getLogger().warning("Error during FreeMinecraftModels setup: " + e.getMessage());
        }
        // Sets up game
        setupGame();
        getServer().getPluginManager().registerEvents(new eventlisteners(this), this);
        snailcommands commandExecutor = new snailcommands(this);
        this.getCommand("snail").setExecutor(commandExecutor);
        this.getCommand("snail").setTabCompleter(commandExecutor);
        getLogger().info("Snail Enabled");
    }

    public void setupGame() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Calls methods to setup the minigame
            restrictPlayerArea(player);
            grantEffects(player);
        }

        // Enforces area restriction globally every second
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasMetadata("restrictedArea")) {
                    restrictPlayerArea(player);
                }
            }
        }, 0L, 20L); // Run every second
    }

    public void resetGame(CommandSender sender) {
        stopSnails();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            Location spawn = player.getWorld().getSpawnLocation();
            player.teleport(spawn);
            player.setMetadata("restrictedArea", new FixedMetadataValue(this, true));
            restrictPlayerArea(player);
            grantEffects(player);
            player.sendMessage(ChatColor.YELLOW + "You have been reset to the beginning!");
        }
    }

    // Stops immortal snails
    public void stopSnails() {
        snailActive = false;
        for (DynamicEntity snail : playerSnailMap.values()) {
            if (snail != null && snail.getLivingEntity().isValid()) {
                snail.remove();
            }
        }
        playerSnailMap.clear();
    }

    // Restricts players until the game starts
    public void restrictPlayerArea(Player player) {
        Location center = player.getWorld().getSpawnLocation();
        double maxDistance = 5;

        // Add metadata if not already present
        if (!player.hasMetadata("restrictedArea")) {
            player.setMetadata("restrictedArea", new FixedMetadataValue(this, true));
        }

        // Enforce teleport if outside bounds
        if (player.getLocation().distance(center) > maxDistance) {
            player.teleport(center);
        }
    }

    // Grants player effects
    public void grantEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 1, true, false));
    }

    // Resets effects and gamemode
    public void resetPlayerEffectsAndMode() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Remove regen and saturation
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SATURATION);

            // Set player to survival
            player.setGameMode(GameMode.SURVIVAL);

            // Remove area restriction
            removeAreaRestriction(player);
        }
    }

    // Removes area restriction
    public void removeAreaRestriction(Player player) {
        player.removeMetadata("restrictedArea", this);
    }

    public void spawnSnailsForPlayers() {
        snailActive = true;
        String modelID = "snail";

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Calculate spawn location behind the player
            Location spawnLocation = player.getLocation().clone()
                    .add(player.getLocation().getDirection().normalize().multiply(-30));
            spawnLocation.setY(spawnLocation.getWorld().getHighestBlockYAt(spawnLocation) + 1);

            // Spawn a pig as the host entity
            Pig host = player.getWorld().spawn(spawnLocation, Pig.class);
            host.setInvisible(true);
            host.setSilent(true);
            host.setAI(true); //Enable pathfinding ai
            host.setRemoveWhenFarAway(false); // Prevent despawning
            host.setGravity(true); // Keep gravity enabled

            host.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.0);

            getLogger().info("DEBUG: Spawned pig for " + player.getName() + " at " + spawnLocation);

            // Create DynamicEntity with the snail model
            DynamicEntity snail = DynamicEntity.create("snail", host);
            snail.setName(player.getName() + "'s Snail");
            snail.setNameVisible(true);

            // Store the pairing
            playerSnailMap.put(player, snail);

            getLogger().info("DEBUG: Created DynamicEntity for " + player.getName());

            // Try NMS pathfinding first
            boolean nmsSuccess = false;
            try {
                nmsSuccess = SnailNMS.addPathfindingToEntity(host, player);
                if (nmsSuccess) {
                    getLogger().info("DEBUG: Successfully applied NMS pathfinding for " + player.getName());
                }
            } catch (Exception e) {
                getLogger().warning("NMS pathfinding failed: " + e.getMessage());
                nmsSuccess = false;
            }

            // If NMS fails, use simple velocity-based following
            if (!nmsSuccess) {
                getLogger().info("DEBUG: Using simple follower for " + player.getName());
                SimpleSnailFollower.startFollowing(host, player, this);
            }

            player.sendMessage(ChatColor.GREEN + "Your Immortal Snail has been summoned.");
        }
    }

    /**
     * Backup follow logic in case NMS fails
     */
    private void startBackupFollowingLogic(org.bukkit.entity.Entity snailEntity, Player player) {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!snailEntity.isValid() || !player.isOnline() || !snailActive) return;

            // Stop the task if the game is no longer active
            if (!snailActive) return;

            Location playerLoc = player.getLocation();
            Location snailLoc = snailEntity.getLocation();

            double distance = snailLoc.distance(playerLoc);

            if (distance > 30) {
                // Teleport snail if it's too far away
                Location teleportLoc = playerLoc.clone().add(
                        player.getLocation().getDirection().normalize().multiply(-20));
                teleportLoc.setY(teleportLoc.getWorld().getHighestBlockYAt(teleportLoc) + 1);
                snailEntity.teleport(teleportLoc);
            } else if (distance > 2) {
                // If not too close, move toward player
                Vector direction = playerLoc.toVector().subtract(snailLoc.toVector()).normalize().multiply(0.05);
                snailEntity.setVelocity(direction);
            }
        }, 0L, 10L); // Every 0.5s
    }

    /**
     * For backward compatibility
     */
    public void startFollowingSnail(org.bukkit.entity.LivingEntity snail, Player player) {
        startBackupFollowingLogic(snail, player);
    }
}