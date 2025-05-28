package me.ghostgeorge.immortalSnail_Maven.commands;

import me.ghostgeorge.immortalSnail_Maven.ImmortalSnail_Maven;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class snailcommands implements CommandExecutor, TabExecutor {
    private final ImmortalSnail_Maven plugin;

    public snailcommands(ImmortalSnail_Maven plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If snail command is run
        if (command.getName().equalsIgnoreCase("snail")) {
            // Checks for permissions to run command
            if (sender instanceof Player && !sender.hasPermission("immortalsnail.admin")) {
                sender.sendMessage("You do not have permission to run this command.");
                return false;
            }
            // If user has permissions
            if (args.length == 1) {
                // If command arg was start
                if (args[0].equalsIgnoreCase("start")) {
                    // Takes away starting effects
                    plugin.resetPlayerEffectsAndMode();
                    // Removes area restriction for all players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.removeAreaRestriction(player);
                    }
                    // Spawns a snail for each player
                    plugin.spawnSnailsForPlayers();
                    // Inform players the snails are starting
                    sender.sendMessage(ChatColor.GREEN + "The Immortal Snails have been unleashed!");
                    return true;
                }
                // If command arg was reset
                if (args[0].equalsIgnoreCase("reset")) {
                    // Executes reset game method
                    plugin.resetGame(sender);
                    sender.sendMessage(ChatColor.YELLOW + "Game has been reset. All players returned to start.");
                    return true;
                }
                // If command arg was pause
                if (args[0].equalsIgnoreCase("pause")) {
                    if (!plugin.snailPaused) {
                        // Pausing the game
                        plugin.snailPaused = true;
                        Bukkit.broadcastMessage(ChatColor.RED + "Game has been paused.");
                    } else {
                        // Unpausing the game
                        plugin.snailPaused = false;
                        Bukkit.broadcastMessage(ChatColor.GREEN + "Game has been unpaused.");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        final List<String> validArguments = new ArrayList<>();
        if (args.length == 1) {
            // /snail <start | reset | pause>
            StringUtil.copyPartialMatches(args[0], List.of("start", "reset", "pause"), validArguments);
            return validArguments;
        }
        return List.of();
    }
}