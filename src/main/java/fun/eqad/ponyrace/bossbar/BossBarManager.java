package fun.eqad.ponyrace.bossbar;

import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import java.util.*;

public class BossBarManager {
    private final Map<UUID, PlayerDataManager> playerDataMap = new HashMap<>();
    public final Map<UUID, BossBar> staminaBars = new HashMap<>();
    public final Map<UUID, BossBar> manaBars = new HashMap<>();

    public void kirinRageBar(Player player, PlayerDataManager data) {
        BossBar bar = manaBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar newBar = Bukkit.createBossBar("§4发怒", BarColor.YELLOW, BarStyle.SEGMENTED_20);
            newBar.addPlayer(player);
            return newBar;
        });

        double progress = data.getEnrageTime() / (double) 100;
        bar.setProgress(progress);

        if (data.isEnraged()) {
            bar.setTitle("§4发怒");
            bar.setColor(BarColor.RED);
        } else {
            bar.setTitle("§7发怒");
            bar.setColor(progress > 0.75 ? BarColor.YELLOW : BarColor.BLUE);
        }
        boolean shouldHide = !data.isEnraged() && progress >= 1.0;

        bar.setVisible(!shouldHide);
    }

    public void initBossBars(Player player, String race) {
        removeBossBars(player.getUniqueId());
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());

        if ("earthpony".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7体力", BarColor.YELLOW, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            staminaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }

        if ("pegasus".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7体力", BarColor.BLUE, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            staminaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }

        if ("nightmare".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7体力", BarColor.GREEN, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            staminaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }

        if ("seapony".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7体力", BarColor.BLUE, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            staminaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }

        if ("dragon".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7体力", BarColor.PINK, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            staminaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }

        if ("unicorn".equals(race)) {
            BossBar bar = Bukkit.createBossBar("§7魔力", BarColor.PURPLE, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            manaBars.put(player.getUniqueId(), bar);
            updateBossBar(player, data, bar, 100, 100);
        }
    }

    public void updateBossBars(Player player, PlayerDataManager data) {
        if ("earthpony".equals(data.getRace())) {
            updateBossBar(player, data, staminaBars.get(player.getUniqueId()), data.getStamina(), 100);
        }

        if ("pegasus".equals(data.getRace())) {
            updateBossBar(player, data, staminaBars.get(player.getUniqueId()), data.getStamina(), 100);
        }

        if ("nightmare".equals(data.getRace())) {
            updateBossBar(player, data, staminaBars.get(player.getUniqueId()), data.getStamina(), 100);
        }

        if ("seapony".equals(data.getRace())) {
            updateBossBar(player, data, staminaBars.get(player.getUniqueId()), data.getStamina(), 100);
        }

        if ("kirin".equals(data.getRace())) {
            kirinRageBar(player, data);
        }

        if ("dragon".equals(data.getRace())) {
            updateBossBar(player, data, staminaBars.get(player.getUniqueId()), data.getStamina(), 100);
        }

        if ("unicorn".equals(data.getRace())) {
            updateBossBar(player, data, manaBars.get(player.getUniqueId()), data.getMana(), 100);
        }
    }

    private void updateBossBar(Player player, PlayerDataManager data, BossBar bar, int current, int max) {
        if (bar == null) return;

        double progress = Math.max(0, Math.min(1.0, (double) current / max));
        bar.setProgress(progress);

        boolean shouldHide = current == max;
        bar.setVisible(!shouldHide);

        if (!shouldHide) {
            if (progress < 0.3) {
                bar.setColor(BarColor.RED);
            } else {
                if ("earthpony".equals(data.getRace())) {
                    bar.setColor(BarColor.YELLOW);
                }

                if ("unicorn".equals(data.getRace())) {
                    bar.setColor(BarColor.PURPLE);
                }

                if ("pegasus".equals(data.getRace())) {
                    bar.setColor(BarColor.BLUE);
                }

                if ("nightmare".equals(data.getRace())) {
                    bar.setColor(BarColor.GREEN);
                }

                if ("seapony".equals(data.getRace())) {
                    bar.setColor(BarColor.BLUE);
                }

                if ("dragon".equals(data.getRace())) {
                    bar.setColor(BarColor.PINK);
                }
            }
        }
    }

    public void removeBossBars(UUID uuid) {
        BossBar bar = staminaBars.remove(uuid);
        if (bar != null) bar.removeAll();

        bar = manaBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }
}