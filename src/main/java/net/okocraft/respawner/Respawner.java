package net.okocraft.respawner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Respawner extends JavaPlugin implements CommandExecutor, TabCompleter {

    private FileConfiguration config;
    private FileConfiguration defaultConfig;

    @Override
    public void onEnable() {
        PluginCommand command = Objects.requireNonNull(getCommand("respawn"));
        command.setExecutor(this);
        command.setTabCompleter(this);
        saveDefaultConfig();

        config = getConfig();
        defaultConfig = getDefaultConfig();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("specify-player"));
            return false;
        }

        List<Player> targets = Bukkit.selectEntities(sender, args[0]).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .collect(Collectors.toList());
        if (targets.isEmpty()) {
            sender.sendMessage(getMessage("player-not-found"));
            return false;
        }

        List<Player> targetDeads = targets.stream()
                .filter(Entity::isDead)
                .collect(Collectors.toList());
        if (targetDeads.isEmpty()) {
            sender.sendMessage(getMessageWithPlayerName(
                    "player-is-alive",
                    targets.stream()
                            .map(Player::getName)
                            .collect(Collectors.joining(", "))
            ));
            return false;
        }

        targetDeads.forEach(target -> {
            target.spigot().respawn();
            target.sendMessage(getMessageWithPlayerName("respawned-by", sender.getName()));
        });

        sender.sendMessage(getMessageWithPlayerName(
                "respawned-player",
                targetDeads.stream()
                        .map(Player::getName)
                        .collect(Collectors.joining(", "))
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(
                    args[0],
                    Stream.concat(
                            Stream.of("@a", "@p", "@r", "@a[distance.."),
                            Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    ).collect(Collectors.toSet()),
                    new ArrayList<>()
            );
        }
        return Collections.emptyList();
    }

    private FileConfiguration getDefaultConfig() {
        InputStream is = getResource("config.yml");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(is));
    }

    private String getMessage(String key) {
        String fullKey = "messages." + key;
        return ChatColor.translateAlternateColorCodes('&', config.getString(fullKey, defaultConfig.getString(fullKey)));
    }

    private String getMessageWithPlayerName(String key, String player) {
        return getMessage(key).replaceAll("%player_name%", player);
    }
}
