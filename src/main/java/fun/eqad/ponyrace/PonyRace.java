package fun.eqad.ponyrace;

import com.google.gson.*;
import fun.eqad.ponyrace.api.*;
import fun.eqad.ponyrace.bossbar.BossBarManager;
import fun.eqad.ponyrace.bstats.bStats;
import fun.eqad.ponyrace.command.CommandManager;
import fun.eqad.ponyrace.config.ConfigManager;
import fun.eqad.ponyrace.event.PlayerEvent;
import fun.eqad.ponyrace.papi.ExpansionManager;
import fun.eqad.ponyrace.playerdata.*;
import fun.eqad.ponyrace.race.RaceSelection;
import fun.eqad.ponyrace.recipe.RecipeManager;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.util.*;

public class PonyRace extends JavaPlugin {
    private ConfigManager config;
    private RecipeManager recipe;
    private BossBarManager bossBar;
    private RaceSelection raceSelection;
    private PlayerEvent playerEvent;
    private PonyRaceAPI api;
    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Location.class, new LocationAdapter())
            .create();
    private final Map<UUID, PlayerDataManager> playerDataMap = new HashMap<>();
    private final Map<UUID, Long> lastBoostTime = new HashMap<>();
    private final Map<UUID, Long> dragonFireCooldown = new HashMap<>();
    private final Map<UUID, Long> EatCooldown = new HashMap<>();

    public ConfigManager getConfigManager() {
        return config;
    }

    public BossBarManager getBossBarManager() {
        return bossBar;
    }

    public RecipeManager getRecipeManager() {
        return recipe;
    }

    public RaceSelection getRaceSelection() {
        return raceSelection;
    }

    public PlayerDataManager getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public Map<UUID, PlayerDataManager> getPlayerDataMap() {
        return playerDataMap;
    }

    @Override
    public void onEnable() {
        getLogger().info("   ___                 ___");
        getLogger().info("  / _ \\___  ___  __ __/ _ \\___ ________");
        getLogger().info(" / ___/ _ \\/ _ \\/ // / , _/ _ `/ __/ -_)");
        getLogger().info("/_/   \\___/_//_/\\_, /_/|_|\\_,_/\\__/\\__/");
        getLogger().info("               /___/");
        getLogger().info("小马大法好!");
        getLogger().info("Author: EQAD Network");
        getLogger().info("使用文档: https://www.eqad.fun/wiki/ponyrace");

        this.config = new ConfigManager(this);
        this.recipe = new RecipeManager(this);
        this.bossBar = new BossBarManager();
        this.raceSelection = new RaceSelection(this);
        this.playerEvent = new PlayerEvent(this, config, bossBar, raceSelection, 
                                          playerDataMap, lastBoostTime, dragonFireCooldown, EatCooldown);

        getServer().getPluginManager().registerEvents(playerEvent, this);
        getServer().getPluginManager().registerEvents(raceSelection, this);

        new bStats(this, 26045);

        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllData();
            }
        }.runTaskTimer(this, 20 * 60, 20 * 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    PlayerDataManager data = playerDataMap.get(uuid);
                    if (data == null) continue;

                    if ("kirin".equals(data.getRace())) {
                        playerEvent.kirinEffects(player, data);
                    }
                }
            }
        }.runTaskTimer(this, 0, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
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
        }.runTaskTimer(this, 0, 20);

        recipe.registerRecipes();

        this.api = new PonyRaceAPIImpl(this);

        getCommand("ponyrace").setExecutor(new CommandManager(this));
        getCommand("ponyrace").setTabCompleter(new CommandManager(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExpansionManager(this).register();
            getLogger().info("检测到PlaceholderAPI, 已启用相关支持");
        }

        getLogger().info("PonyRace已成功加载");
    }

    @Override
    public void onDisable() {
        saveAllData();
        bossBar.staminaBars.values().forEach(BossBar::removeAll);
        bossBar.manaBars.values().forEach(BossBar::removeAll);
        Bukkit.resetRecipes();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata("using_rebirth_potion", this);
        }

        getLogger().info("PonyRace已成功卸载");
    }

    public PonyRaceAPI getAPI() {
        return api;
    }

    public void saveData(UUID uuid) {
        try {
            File dataFile = new File(getDataFolder(), "playerdata/" + uuid + ".json");
            dataFile.getParentFile().mkdirs();

            PlayerDataManager data = playerDataMap.get(uuid);
            if (data != null) {
                try (Writer writer = new FileWriter(dataFile)) {
                    gson.toJson(data, writer);
                }
            }
        } catch (IOException e) {
            getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    public PlayerDataManager loadData(UUID uuid) {
        File dataFile = new File(getDataFolder(), "playerdata/" + uuid + ".json");
        if (!dataFile.exists()) return null;

        try (Reader reader = new FileReader(dataFile)) {
            return gson.fromJson(reader, PlayerDataManager.class);
        } catch (IOException e) {
            getLogger().severe("加载玩家数据失败: " + e.getMessage());
        }
        return null;
    }

    private void saveAllData() {
        for (UUID uuid : playerDataMap.keySet()) {
            saveData(uuid);
        }
    }
}