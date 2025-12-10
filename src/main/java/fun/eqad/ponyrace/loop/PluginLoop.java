package fun.eqad.ponyrace.loop;

import fun.eqad.ponyrace.PonyRace;
import fun.eqad.ponyrace.event.PlayerEvent;
import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class PluginLoop {
    private final PonyRace plugin;
    private final PlayerEvent playerEvent;
    private final Map<UUID, PlayerDataManager> playerDataMap;

    public PluginLoop(PonyRace plugin, PlayerEvent playerEvent, Map<UUID, PlayerDataManager> playerDataMap) {
        this.plugin = plugin;
        this.playerEvent = playerEvent;
        this.playerDataMap = playerDataMap;
    }

    public void startLoops() {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerDataManager.saveAllData(plugin, playerDataMap);
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerDataManager data = playerDataMap.get(uuid);
                    if (data == null) continue;

                    if ("kirin".equals(data.getRace())) {
                        playerEvent.kirinEffects(player, data);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerDataManager data = playerDataMap.get(uuid);
                    if (data == null) continue;

                    if ("dragon".equals(data.getRace())) {
                        playerEvent.dragonEffects(player, data);
                    }

                    if ("earthpony".equals(data.getRace())) {
                        playerEvent.earthPonyEffects(player);
                    }

                    if ("pegasus".equals(data.getRace())) {
                        playerEvent.pegasusEffects(player, data);
                    }

                    if ("unicorn".equals(data.getRace())) {
                        playerEvent.unicornMana(player, data);
                    }

                    if ("nightmare".equals(data.getRace())) {
                        playerEvent.nightmareEffects(player, data);
                    }

                    if ("seapony".equals(data.getRace())) {
                        playerEvent.seaponyEffects(player, data);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}