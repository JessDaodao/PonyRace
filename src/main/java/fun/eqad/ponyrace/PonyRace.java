package fun.eqad.ponyrace;

import com.google.gson.*;
import fun.eqad.ponyrace.api.*;
import fun.eqad.ponyrace.api.events.*;
import fun.eqad.ponyrace.bossbar.BossBarManager;
import fun.eqad.ponyrace.bstats.bStats;
import fun.eqad.ponyrace.command.CommandManager;
import fun.eqad.ponyrace.config.ConfigManager;
import fun.eqad.ponyrace.papi.ExpansionManager;
import fun.eqad.ponyrace.playerdata.*;
import fun.eqad.ponyrace.recipe.RecipeManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.Vector;
import java.io.*;
import java.util.*;

public class PonyRace extends JavaPlugin implements Listener {
    private ConfigManager config;
    private RecipeManager recipe;
    private BossBarManager bossBar;
    private PonyRaceAPI api;
    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Location.class, new LocationAdapter())
            .create();
    private final Map<UUID, PlayerDataManager> playerDataMap = new HashMap<>();
    private final Map<UUID, Long> lastBoostTime = new HashMap<>();
    private final Map<UUID, Long> dragonFireCooldown = new HashMap<>();
    private final Map<UUID, Long> EatCooldown = new HashMap<>();
    private final Set<UUID> selectingPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> selectionTasks = new HashMap<>();

    public ConfigManager getConfigManager() {
        return config;
    }

    public BossBarManager getBossBarManager() {
        return bossBar;
    }

    public RecipeManager getRecipeManager() {
        return recipe;
    }

    public PlayerDataManager getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
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

        getServer().getPluginManager().registerEvents(this, this);

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
                        kirinEffects(player, data);
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
                        dragonEffects(player, data);
                    }

                    if ("earthpony".equals(data.getRace())) {
                        earthPonyEffects(player);
                    }

                    if ("pegasus".equals(data.getRace())) {
                        pegasusEffects(player, data);
                    }

                    if ("unicorn".equals(data.getRace())) {
                        unicornMana(player, data);
                    }

                    if ("nightmare".equals(data.getRace())) {
                        nightmareEffects(player, data);
                    }

                    if ("seapony".equals(data.getRace())) {
                        seaponyEffects(player, data);
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerDataManager data = loadData(uuid);
        if (data == null) {
            data = new PlayerDataManager(uuid);
        }

        playerDataMap.put(uuid, data);

        if (!data.isHasChosen()) {
            if (config.shouldShowSelection()) {
                if (config.shouldLoginPluginSupport()) {
                    loginPluginSpecialSelection(player);
                } else {
                    openRaceSelection(player);
                }
            }
            if (config.shouldForceSelection() && !config.shouldLoginPluginSupport()) {
                selectingPlayers.add(player.getUniqueId());
            }
        } else {
            bossBar.initBossBars(player, data.getRace());
            bossBar.updateBossBars(player, data);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        cancelTask(event.getPlayer().getUniqueId());
        selectingPlayers.remove(player.getUniqueId());
        bossBar.removeBossBars(uuid);
        saveData(uuid);
        playerDataMap.remove(uuid);
    }

    private void loginPluginSpecialSelection(Player player) {
        UUID uuid = player.getUniqueId();

        if (selectionTasks.containsKey(uuid)) {
            selectionTasks.get(uuid).cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() ||
                        playerDataMap.get(uuid).isHasChosen()) {
                    cancelTask(uuid);
                    return;
                }
                openRaceSelection(player);
            }
        }.runTaskTimer(this, 0, 5);

        selectionTasks.put(uuid, task);
    }

    private void cancelTask(UUID uuid) {
        if (selectionTasks.containsKey(uuid)) {
            selectionTasks.get(uuid).cancel();
            selectionTasks.remove(uuid);
        }
    }

    public void openRaceSelection(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, "选择你的种族 §7>>>");

        ItemStack pegasus = createItem(Material.FEATHER, "§b飞马",
                Arrays.asList(
                        "§8--------",
                        "§7永久速度I效果",
                        "§7拿着木棍时能飞行",
                        "§7飞行时拿着木棍右键可以俯冲",
                        "§7飞行和俯冲有体力限制",
                        "§7喜欢素食, 讨厌肉食",
                        "§7可以食用一些原版不能直接食用的植物",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack earthPony = createItem(Material.WHEAT, "§6陆马",
                Arrays.asList(
                        "§8--------",
                        "§724点生命上限",
                        "§7永久力量I效果",
                        "§7永久跳跃提升I效果",
                        "§7永久速度I效果",
                        "§7在地面上拿着木棍右键可以冲刺",
                        "§7冲刺有体力限制",
                        "§7喜欢素食, 讨厌肉食",
                        "§7可以食用一些原版不能直接食用的植物",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack unicorn = createItem(Material.AMETHYST_SHARD, "§d独角兽",
                Arrays.asList(
                        "§8--------",
                        "§7拿着木棍左键可以发射激光 (可以穿透)",
                        "§7拿着木棍右键可以传送到你看着的方块上方",
                        "§7发射激光和传送有魔力限制",
                        "§7喜欢素食, 讨厌肉食",
                        "§7可以食用一些原版不能直接食用的植物",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack nightmare = createItem(Material.ENDER_EYE, "§2夜琪",
                Arrays.asList(
                        "§8--------",
                        "§7拿着木棍时能飞行",
                        "§7飞行时拿着木棍右键可以俯冲",
                        "§7飞行和俯冲有体力限制",
                        "§7拥有夜视能力",
                        "§7白天有虚弱I效果",
                        "§7晚上有力量II效果",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack seapony = createItem(Material.HEART_OF_THE_SEA, "§3海马",
                Arrays.asList(
                        "§8--------",
                        "§7可以在水下呼吸",
                        "§7水下游泳速度更快",
                        "§7在水中拿着木棍右键可以突进",
                        "§7突进有体力限制",
                        "§7喜欢素食, 讨厌肉食",
                        "§7可以食用一些原版不能直接食用的植物",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack kirin = createItem(Material.FIRE_CHARGE, "§e麒麟",
                Arrays.asList(
                        "§8--------",
                        "§730点生命上限",
                        "§7不怕火",
                        "§7拿着木棍右键可以发怒",
                        "§7处于发怒状态时会获得力量II和抗性提升I效果",
                        "§7发怒后会获得时长为3分钟的虚弱II效果",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack dragon = createItem(Material.LAVA_BUCKET, "§4龙族",
                Arrays.asList(
                        "§8--------",
                        "§726点生命上限",
                        "§7不怕火",
                        "§7拿着木棍时能飞行",
                        "§7拿着木棍左键可以喷火球",
                        "§7飞行有体力限制",
                        "§7吃绿宝石/钻石可以恢复生命和饥饿值",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));
        ItemStack human = createItem(Material.LEATHER_BOOTS, "§7人类",
                Arrays.asList(
                        "§8--------",
                        "§7无任何附加属性",
                        "§8--------",
                        "§a点击选择",
                        "§8--------"
                ));

        inv.setItem(10, pegasus);
        inv.setItem(12, earthPony);
        inv.setItem(14, unicorn);
        inv.setItem(16, nightmare);
        inv.setItem(28, seapony);
        inv.setItem(30, kirin);
        inv.setItem(32, dragon);
        inv.setItem(34, human);

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("选择你的种族 §7>>>")) return;

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        String race = null;
        switch (clicked.getType()) {
            case FEATHER: race = "pegasus"; break;
            case WHEAT: race = "earthpony"; break;
            case AMETHYST_SHARD: race = "unicorn"; break;
            case ENDER_EYE: race = "nightmare"; break;
            case HEART_OF_THE_SEA: race = "seapony"; break;
            case FIRE_CHARGE: race = "kirin"; break;
            case LAVA_BUCKET: race = "dragon"; break;
            case LEATHER_BOOTS: race = "human"; break;
        }

        if (race != null) {
            if (player.hasMetadata("using_rebirth_potion")) {
                player.removeMetadata("using_rebirth_potion", this);

                ItemStack item = player.getInventory().getItemInMainHand();
                ItemMeta meta = item.getItemMeta();
                if (item != null && item.getType() == Material.POTION && meta.getCustomModelData() == 389000) {
                    item.setAmount(item.getAmount() - 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);
                } else {
                    item = player.getInventory().getItemInOffHand();
                    if (item != null && item.getType() == Material.POTION && meta.getCustomModelData() == 389000) {
                        item.setAmount(item.getAmount() - 1);
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);
                    }
                }
            }

            changeRace(player, race);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (selectingPlayers.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (selectingPlayers.contains(player.getUniqueId())) {
                    openRaceSelection(player);
                }
            }, 1L);
        }
    }

    public void changeRace(Player player, String race) {
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        String oldRace = data != null ? data.getRace() : null;

        PlayerRaceChangeEvent event = new PlayerRaceChangeEvent(player, oldRace, race);
        Bukkit.getPluginManager().callEvent(event);

        if (data == null) {
            data = new PlayerDataManager(player.getUniqueId());
            playerDataMap.put(player.getUniqueId(), data);
        }

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        raceSelection(player, race);

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        player.setAllowFlight(false);
        player.setFlying(false);
    }

    private void raceSelection(Player player, String race) {
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        data.setRace(race);
        data.setHasChosen(true);
        data.setStamina(100);
        data.setMana(100);
        data.setEnrageTime(100);
        data.setEnraged(false);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);

        cancelTask(player.getUniqueId());
        selectingPlayers.remove(player.getUniqueId());
        bossBar.removeBossBars(player.getUniqueId());
        bossBar.initBossBars(player, race);

        switch (race) {
            case "pegasus":
                player.sendMessage(config.getMessagePrefix() + "§b你已转生为飞马!");
                break;
            case "earthpony":
                player.sendMessage(config.getMessagePrefix() + "§6你已转生为陆马!");
                break;
            case "unicorn":
                player.sendMessage(config.getMessagePrefix() + "§d你已转生为独角兽!");
                break;
            case "nightmare":
                player.sendMessage(config.getMessagePrefix() + "§2你已转生为夜琪!");
                break;
            case "seapony":
                player.sendMessage(config.getMessagePrefix() + "§3你已转生为海马!");
                break;
            case "kirin":
                player.sendMessage(config.getMessagePrefix() + "§e你已转生为麒麟!");
                break;
            case "dragon":
                player.sendMessage(config.getMessagePrefix() + "§4你已转生为龙族!");
                break;
            case "human":
                player.sendMessage(config.getMessagePrefix() + "§7你已转生为人类!");
                break;
        }

        saveData(player.getUniqueId());
    }

    private boolean cantUseSkill(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (offHand.getType() == Material.STICK && mainHand.getType() == Material.AIR) {
            return true;
        }

        return mainHand.getType() == Material.STICK;
    }

    private boolean isHoldingStick(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.STICK ||
                player.getInventory().getItemInOffHand().getType() == Material.STICK;
    }

    private final Set<Material> specialPonyFoods = Set.of(
            Material.WHEAT, Material.GRASS, Material.TALL_GRASS, Material.FERN,
            Material.LARGE_FERN, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH,
            Material.PEONY
    );

    private final Set<Material> vegetarianFoods = Set.of(
            Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
            Material.CARROT, Material.GOLDEN_CARROT, Material.POTATO, Material.BAKED_POTATO,
            Material.BEETROOT, Material.BEETROOT_SOUP, Material.BREAD, Material.COOKIE,
            Material.MELON_SLICE, Material.PUMPKIN_PIE, Material.SWEET_BERRIES,
            Material.GLOW_BERRIES, Material.DRIED_KELP, Material.HONEY_BOTTLE
    );

    private final Set<Material> meatFoods = Set.of(
            Material.BEEF, Material.COOKED_BEEF, Material.PORKCHOP, Material.COOKED_PORKCHOP,
            Material.CHICKEN, Material.COOKED_CHICKEN, Material.MUTTON, Material.COOKED_MUTTON,
            Material.RABBIT, Material.COOKED_RABBIT, Material.COD, Material.COOKED_COD,
            Material.SALMON, Material.COOKED_SALMON, Material.ROTTEN_FLESH, Material.SPIDER_EYE
    );

    @EventHandler
    public void ponyEat(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !specialPonyFoods.contains(item.getType())) {
            return;
        }

        if (player.getFoodLevel() >= 20) {
            return;
        }

        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        if (data == null || !Arrays.asList("pegasus", "earthpony", "unicorn", "seapony").contains(data.getRace())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (EatCooldown.containsKey(player.getUniqueId())) {
            long elapsed = now - EatCooldown.get(player.getUniqueId());
            if (elapsed < config.getEatCooldown()) {
                return;
            }
        }

        item.setAmount(item.getAmount() - 1);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SATURATION, 1, 1, false, false, false));

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);

        EatCooldown.put(player.getUniqueId(), now);
    }

    @EventHandler
    public void onFoodConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());

        if (data == null || !Arrays.asList("pegasus", "earthpony", "unicorn", "seapony").contains(data.getRace())) {
            return;
        }

        if (vegetarianFoods.contains(item.getType())) {
            int foodLevel = player.getFoodLevel();
            player.setFoodLevel(Math.min(foodLevel + 4, 20));

            float saturation = player.getSaturation();
            player.setSaturation(Math.min(saturation + 6.0f, 20.0f));
        } else if (meatFoods.contains(item.getType())) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HUNGER, 900, 30, false, false, true));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONFUSION, 900, 0, false, false, true));
        }
    }

    private void dragonEffects(Player player, PlayerDataManager data) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(26);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false, false));

        ponyFly(player, data);
    }

    @EventHandler
    public void dragonEat(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        if (data == null || !"dragon".equals(data.getRace())) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Material mat = item.getType();
        if ((mat == Material.EMERALD || mat == Material.DIAMOND) &&
                event.getAction() == Action.RIGHT_CLICK_AIR) {

            long now = System.currentTimeMillis();
            if (EatCooldown.containsKey(player.getUniqueId())) {
                long elapsed = now - EatCooldown.get(player.getUniqueId());
                if (elapsed < config.getEatCooldown()) {
                    return;
                }
            }

            double health = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double restore = mat == Material.EMERALD ? 8 : 16;

            if (health >= maxHealth) {
                return;
            }

            player.setHealth(Math.min(health + restore, maxHealth));
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION, 4, 0, false, false, false));
            item.setAmount(item.getAmount() - 1);

            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1, 0.8f);

            EatCooldown.put(player.getUniqueId(), now);
        }
    }

    private void dragonFire(Player player) {
        long now = System.currentTimeMillis();

        if (dragonFireCooldown.containsKey(player.getUniqueId())) {
            long elapsed = now - dragonFireCooldown.get(player.getUniqueId());
            if (elapsed < config.getDragonFireCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c技能冷却中, 剩余" + String.format("%.1f秒", (config.getDragonFireCooldown() - elapsed)/1000.0))
                );
                return;
            }
        }

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location startLoc = player.getEyeLocation().add(direction.multiply(1));

        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(0.5));
        fireball.setIsIncendiary(true);
        fireball.setYield(2f);

        fireball.getWorld().spawnParticle(Particle.DRAGON_BREATH, startLoc, 20, 0.1, 0.1, 0.1, 0.05);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1, 1.2f);

        dragonFireCooldown.put(player.getUniqueId(), now);
    }

    private void kirinEffects(Player player, PlayerDataManager data) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false, false));

        if (data.isEnraged()) {
            data.setEnrageTime(data.getEnrageTime() - config.getEnrageConsume());

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INCREASE_DAMAGE, 200, 1, false, false, false));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DAMAGE_RESISTANCE, 200, 0, false, false, false));

            player.getWorld().spawnParticle(Particle.LAVA,
                    player.getLocation(), 30, 1, 0.5, 1, 0.2);

            if (player.isInWater()) {
                data.setEnraged(false);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷水使你冷静了下来...")
                );

                player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, 3600, 0, false, false, false));
            }

            if (data.getEnrageTime() <= 0) {
                data.setEnraged(false);
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c你冷静了下来...")
                );

                player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, 3600, 0, false, false, false));
            }

            bossBar.updateBossBars(player, data);
        } else if (data.getEnrageTime() < 100) {
            data.setEnrageTime(Math.min(100,
                    data.getEnrageTime() + config.getEnrageRegen()));
            bossBar.updateBossBars(player, data);
        }
    }

    private void kirinRage(Player player, PlayerDataManager data) {
        if (data.isEnraged()) {
            return;
        }

        if (data.getEnrageTime() < config.getEnrageCooldown()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c冷却中")
            );
            return;
        }

        data.setEnraged(true);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("§c你发怒了!")
        );

        player.removePotionEffect(PotionEffectType.WEAKNESS);

        player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 30, 1, 0.5, 1, 0.2);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.8f);
    }

    private void seaponyEffects(Player player, PlayerDataManager data) {
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WATER_BREATHING, 200, 0, false, false, false));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE, 200, 0, false, false, false));

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
        }

        if (!data.isUsingAbility() && data.getStamina() < 100) {
            data.setStamina(Math.min(100, data.getStamina() + config.getStaminaRegen()));
            bossBar.updateBossBars(player, data);
        }
    }

    private void seaponyBoost(Player player, PlayerDataManager data) {
        long now = System.currentTimeMillis();
        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getBoostCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        if (data.getStamina() < config.getStaminaBoostConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c体力不足")
            );
            return;
        }

        data.setStamina(data.getStamina() - config.getStaminaBoostConsume());
        data.setUsingAbility(true);

        Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(2.5));

        player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1, 1.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUsingAbility(false);
            }
        }.runTaskLater(this, 60);

        lastBoostTime.put(player.getUniqueId(), now);

        bossBar.updateBossBars(player, data);
    }

    private void nightmareEffects(Player player, PlayerDataManager data) {
        World world = player.getWorld();
        long time = world.getTime();
        boolean isNight = time >= 13100 && time <= 22800;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));

        if (isNight) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INCREASE_DAMAGE, 200, 1, false, false, false));
        }

        if (!isNight) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS, 200, 0, false, false, false));
        }

        ponyFly(player, data);
    }

    private void nightmareBoost(Player player, PlayerDataManager data) {
        long now = System.currentTimeMillis();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getBoostCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        if (data.getStamina() < config.getStaminaBoostConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c体力不足")
            );
            return;
        }

        data.setStamina(data.getStamina() - config.getStaminaBoostConsume());

        Vector direction = player.getLocation().getDirection().normalize();

        Vector velocity = player.getVelocity();
        velocity.setX(direction.getX() * 2.5);
        velocity.setZ(direction.getZ() * 2.5);

        player.setVelocity(velocity);

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.2);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1.5f);

        lastBoostTime.put(player.getUniqueId(), now);

        bossBar.updateBossBars(player, data);
    }

    private void pegasusBoost(Player player, PlayerDataManager data) {
        long now = System.currentTimeMillis();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getBoostCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        if (data.getStamina() <= config.getStaminaBoostConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c体力不足")
            );
            return;
        }

        data.setStamina(data.getStamina() - config.getStaminaBoostConsume());

        Vector direction = player.getLocation().getDirection().normalize();

        Vector velocity = player.getVelocity();
        velocity.setX(direction.getX() * 2.5);
        velocity.setZ(direction.getZ() * 2.5);

        player.setVelocity(velocity);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1.5f);

        lastBoostTime.put(player.getUniqueId(), now);

        bossBar.updateBossBars(player, data);
    }

    private void pegasusEffects(Player player, PlayerDataManager data) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 200, 0, false, false, false));

        ponyFly(player, data);
    }

    private void ponyFly(Player player, PlayerDataManager data) {
        BossBar bar = bossBar.staminaBars.get(player.getUniqueId());
        if (bar == null) return;

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            bar.setVisible(false);
            return;
        }

        boolean holdingStick = isHoldingStick(player);

        if (player.isDead()) return;

        if (!player.isFlying() && player.isOnGround()) {
            int regen = config.getStaminaRegen();
            if (regen > 0) {
                data.setStamina(Math.min(100, data.getStamina() + regen));

                if (data.getStamina() >= config.getFlyCooldown() && data.isStaminaExhausted()) {
                    data.setStaminaExhausted(false);
                }
            }
        }

        boolean canFly = holdingStick && !player.isInWater();

        if (data.isStaminaExhausted()) {
            canFly = canFly && data.getStamina() >= config.getFlyCooldown();
        }

        if (canFly) {
            player.setAllowFlight(true);

            if (player.isFlying()) {
                int moveCost = flightMoveCost(player, data);

                int totalCost = config.getStaminaConsume() + moveCost;

                data.setStamina(Math.max(0, data.getStamina() - totalCost));

                data.setLastFlightLocation(player.getLocation());
                data.setLastFlightMoveTime(System.currentTimeMillis());
                data.setLastWorld(player.getWorld().getName());

                if (data.getStamina() <= 0) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                    data.setStaminaExhausted(true);
                    player.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            new TextComponent("§c体力耗尽")
                    );
                }
            }
        } else {
            player.setAllowFlight(false);
        }

        bossBar.updateBossBars(player, data);
    }

    private int flightMoveCost(Player player, PlayerDataManager data) {
        String currentWorld = player.getWorld().getName();

        if (data.getLastWorld() != null &&
                !data.getLastWorld().equals(currentWorld)) {
            data.setLastFlightLocation(null);
            data.setLastFlightMoveTime(0);
            data.setLastWorld(currentWorld);
            return 0;
        }

        data.setLastWorld(currentWorld);

        if (data.getLastFlightLocation() == null || data.getLastFlightMoveTime() == 0) {
            data.setLastFlightLocation(player.getLocation());
            data.setLastFlightMoveTime(System.currentTimeMillis());
            return 0;
        }

        long timeDiff = System.currentTimeMillis() - data.getLastFlightMoveTime();
        double seconds = timeDiff / 1000.0;

        Location lastLoc = data.getLastFlightLocation();
        double distance = player.getLocation().distance(lastLoc);

        double speed = distance / seconds;

        int speedThreshold = config.getStaminaMoveThresholdConsume();

        if (speed < speedThreshold) {
            return 0;
        }

        double extraCost = (speed - speedThreshold) * config.getStaminaMoveConsume();

        return (int) Math.min(extraCost, config.getStaminaMoveMaxConsume());
    }

    private void earthPonyEffects(Player player) {
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(24);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INCREASE_DAMAGE, 200, 0, false, false, false));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP, 200, 0, false, false, false));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, 200, 0, false, false, false));

        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        if (data != null && !data.isUsingAbility() && data.getStamina() < 100) {
            data.setStamina(Math.min(100, data.getStamina() + config.getStaminaRegen()));
            bossBar.updateBossBars(player, data);
        }
    }

    private void earthPonyDash(Player player, PlayerDataManager data) {
        long now = System.currentTimeMillis();

        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getBoostCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        if (data.getStamina() < config.getStaminaBoostConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c体力不足")
            );
            return;
        }

        data.setStamina(data.getStamina() - config.getStaminaBoostConsume());

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(3.0));

        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP, 1, 1.0f);

        lastBoostTime.put(player.getUniqueId(), now);

        data.setUsingAbility(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUsingAbility(false);
            }
        }.runTaskLater(this, 20);

        bossBar.updateBossBars(player, data);
    }

    private void unicornMana(Player player, PlayerDataManager data) {
        BossBar bar = bossBar.manaBars.get(player.getUniqueId());
        if (bar == null) return;

        if (data.getMana() < 100) {
            data.setMana(data.getMana() + config.getManaRegen());
            bossBar.updateBossBars(player, data);
        }
    }

    private void unicornLaser(Player player, PlayerDataManager data) {
        if (data.getMana() < config.getManaLeftConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c魔力不足")
            );
            return;
        }

        long now = System.currentTimeMillis();
        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getManaCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        data.setMana(data.getMana() - config.getManaLeftConsume());
        lastBoostTime.put(player.getUniqueId(), now);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.5f);

        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 5) {
                    this.cancel();
                    return;
                }

                for (double distance = 0; distance <= config.getMaxLaserLength(); distance += 1.0) {
                    Location current = start.clone().add(direction.clone().multiply(distance));

                    player.getWorld().spawnParticle(Particle.REDSTONE, current, 1,
                            new Particle.DustOptions(Color.fromRGB(138, 43, 226), 1.0f));

                    if (ticks == 1) {
                        for (Entity entity : player.getWorld().getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                            if (entity != player && entity instanceof LivingEntity) {
                                LivingEntity livingEntity = (LivingEntity) entity;
                                livingEntity.damage(config.getLaserDamage(), player);
                                livingEntity.getWorld().spawnParticle(Particle.CRIT,
                                        livingEntity.getLocation(), 5);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 2);

        bossBar.updateBossBars(player, data);
    }

    private void unicornTeleport(Player player, PlayerDataManager data) {
        if (data.getMana() < config.getManaRightConsume()) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c魔力不足")
            );
            return;
        }

        long now = System.currentTimeMillis();
        if (lastBoostTime.containsKey(player.getUniqueId())) {
            long elapsed = now - lastBoostTime.get(player.getUniqueId());
            if (elapsed < config.getManaCooldown()) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent("§c冷却中")
                );
                return;
            }
        }

        Location targetLoc = getTargetLocation(player, config.getMaxTeleportLength());

        if (targetLoc == null) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("§c传送目的地不合法")
            );
            return;
        }

        data.setMana(data.getMana() - config.getManaRightConsume());
        lastBoostTime.put(player.getUniqueId(), now);

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.5f);

        player.teleport(targetLoc);

        player.getWorld().spawnParticle(Particle.PORTAL, targetLoc, 50, 0.5, 0.5, 0.5, 0.5);
        player.playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.5f);

        bossBar.updateBossBars(player, data);
    }

    private Location getTargetLocation(Player player, int maxDistance) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        for (int i = 1; i <= maxDistance; i++) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(i));

            if (!checkLoc.getBlock().isPassable()) {
                Location standingLoc = findSafeLocation(checkLoc);
                if (standingLoc != null) {
                    return standingLoc;
                }
                return null;
            }

            if (i == maxDistance) {
                return findSafeLocation(checkLoc);
            }
        }
        return null;
    }

    private Location findSafeLocation(Location loc) {
        for (int y = 0; y >= -5; y--) {
            Location check = loc.clone().add(0, y, 0);
            if (check.getBlock().getType().isSolid() &&
                    check.clone().add(0, 1, 0).getBlock().isPassable() &&
                    check.clone().add(0, 2, 0).getBlock().isPassable()) {
                return check.clone().add(0, 1, 0);
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());

        data.setStamina(100);
        data.setMana(100);
        data.setEnrageTime(100);
        data.setEnraged(false);
        bossBar.updateBossBars(player, data);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        PlayerDataManager data = playerDataMap.get(player.getUniqueId());
        if (data == null) return;

        if (item != null && item.getType() == Material.POTION) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getCustomModelData() == 389000) {
                event.setCancelled(true);

                if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                        event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                    openRaceSelection(player);

                    player.setMetadata("using_rebirth_potion", new FixedMetadataValue(this, true));
                }
                return;
            }

            if (meta.getCustomModelData() == 389001) {
                event.setCancelled(true);

                if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                        event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                    if (!"unicorn".equals(data.getRace())) {
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent("§c你的种族无法饮用此药水")
                        );
                        return;
                    }

                    if (data.getMana() == 100) return;

                    data.setMana(100);
                    bossBar.updateBossBars(player, data);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);
                    item.setAmount(item.getAmount() - 1);
                }
                return;
            }

            if (meta.getCustomModelData() == 389002) {
                event.setCancelled(true);

                if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                        event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                    if (!Arrays.asList("pegasus", "earthpony", "nightmare", "seapony", "dragon").contains(data.getRace())) {
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent("§c你的种族无法饮用此药水")
                        );
                        return;
                    }

                    if (data.getStamina() == 100) return;

                    data.setStamina(100);
                    bossBar.updateBossBars(player, data);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0F, 1.0F);
                    item.setAmount(item.getAmount() - 1);
                }
                return;
            }
        }

        if ("dragon".equals(data.getRace()) &&
                event.getAction() == Action.LEFT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player)) {
            dragonFire(player);
            return;
        }

        if ("kirin".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player) &&
                !player.isInWater()) {
            kirinRage(player, data);
            return;
        }

        if ("seapony".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player) &&
                player.isInWater()) {
            seaponyBoost(player, data);
            return;
        }

        if ("nightmare".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player) &&
                player.isFlying()) {
            nightmareBoost(player, data);
            return;
        }

        if ("pegasus".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player) &&
                player.isFlying()) {
            pegasusBoost(player, data);
            return;
        }

        if ("earthpony".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player) &&
                player.isOnGround() &&
                !player.isInWater()) {
            earthPonyDash(player, data);
            return;
        }

        if ("unicorn".equals(data.getRace()) &&
                event.getAction() == Action.LEFT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player)) {
            unicornLaser(player, data);
        }

        if ("unicorn".equals(data.getRace()) &&
                event.getAction() == Action.RIGHT_CLICK_AIR &&
                isHoldingStick(player) &&
                cantUseSkill(player)) {
            unicornTeleport(player, data);
        }
    }

    private void saveData(UUID uuid) {
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

    private PlayerDataManager loadData(UUID uuid) {
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