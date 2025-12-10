package fun.eqad.ponyrace;

import fun.eqad.ponyrace.api.*;
import fun.eqad.ponyrace.bossbar.BossBarManager;
import fun.eqad.ponyrace.bstats.bStats;
import fun.eqad.ponyrace.command.CommandManager;
import fun.eqad.ponyrace.config.ConfigManager;
import fun.eqad.ponyrace.event.PlayerEvent;
import fun.eqad.ponyrace.loop.PluginLoop;
import fun.eqad.ponyrace.papi.ExpansionManager;
import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import fun.eqad.ponyrace.race.RaceSelection;
import fun.eqad.ponyrace.recipe.RecipeManager;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class PonyRace extends JavaPlugin {
    private ConfigManager config;
    private RecipeManager recipe;
    private BossBarManager bossBar;
    private RaceSelection raceSelection;
    private PlayerEvent playerEvent;
    private PluginLoop pluginLoop;
    private PonyRaceAPI api;
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
        this.pluginLoop = new PluginLoop(this, playerEvent, playerDataMap);

        getServer().getPluginManager().registerEvents(playerEvent, this);
        getServer().getPluginManager().registerEvents(raceSelection, this);

        new bStats(this, 26045);

        pluginLoop.startLoops();

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
        PlayerDataManager.saveAllData(this, playerDataMap);
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
}