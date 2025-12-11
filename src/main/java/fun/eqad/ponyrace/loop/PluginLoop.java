package fun.eqad.ponyrace.loop;

import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import fun.eqad.ponyrace.PonyRace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class PluginLoop {
    private final PonyRace plugin;

    public PluginLoop(PonyRace plugin) {
        this.plugin = plugin;
    }

    public void startLoops() {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager.saveAllData(plugin, plugin.getPlayerDataMap());
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerDataManager data = plugin.getPlayerDataMap().get(uuid);
                    if (data == null) continue;

                    if ("kirin".equals(data.getRace())) {
                        plugin.getRaceEvent().kirinEffects(player, data);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerDataManager data = plugin.getPlayerDataMap().get(uuid);
                    if (data == null) continue;

                    if ("dragon".equals(data.getRace())) {
                        plugin.getRaceEvent().dragonEffects(player, data);
                    }

                    if ("earthpony".equals(data.getRace())) {
                        plugin.getRaceEvent().earthPonyEffects(player);
                    }

                    if ("pegasus".equals(data.getRace())) {
                        plugin.getRaceEvent().pegasusEffects(player, data);
                    }

                    if ("unicorn".equals(data.getRace())) {
                        plugin.getRaceEvent().unicornMana(player, data);
                    }

                    if ("nightmare".equals(data.getRace())) {
                        plugin.getRaceEvent().nightmareEffects(player, data);
                    }

                    if ("seapony".equals(data.getRace())) {
                        plugin.getRaceEvent().seaponyEffects(player, data);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}