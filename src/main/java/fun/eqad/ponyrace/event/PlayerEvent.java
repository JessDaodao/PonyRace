package fun.eqad.ponyrace.event;

import fun.eqad.ponyrace.PonyRace;
import fun.eqad.ponyrace.bossbar.BossBarManager;
import fun.eqad.ponyrace.config.ConfigManager;
import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import fun.eqad.ponyrace.race.RaceSelection;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.Vector;
import java.util.*;

public class PlayerEvent implements Listener {
    private final PonyRace plugin;
    private final ConfigManager config;
    private final BossBarManager bossBar;
    private final RaceSelection raceSelection;
    private final Map<UUID, PlayerDataManager> playerDataMap;
    private final Map<UUID, Long> lastBoostTime;
    private final Map<UUID, Long> dragonFireCooldown;
    private final Map<UUID, Long> EatCooldown;

    public PlayerEvent(PonyRace plugin, ConfigManager config, BossBarManager bossBar, RaceSelection raceSelection,
                      Map<UUID, PlayerDataManager> playerDataMap, Map<UUID, Long> lastBoostTime,
                      Map<UUID, Long> dragonFireCooldown, Map<UUID, Long> EatCooldown) {
        this.plugin = plugin;
        this.config = config;
        this.bossBar = bossBar;
        this.raceSelection = raceSelection;
        this.playerDataMap = playerDataMap;
        this.lastBoostTime = lastBoostTime;
        this.dragonFireCooldown = dragonFireCooldown;
        this.EatCooldown = EatCooldown;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerDataManager data = plugin.loadData(uuid);
        if (data == null) {
            data = new PlayerDataManager(uuid);
        }

        playerDataMap.put(uuid, data);

        if (!data.isHasChosen()) {
            if (config.shouldShowSelection()) {
                if (config.shouldLoginPluginSupport()) {
                    loginPluginSpecialSelection(player);
                } else {
                    raceSelection.openRaceSelection(player);
                }
            }
            if (config.shouldForceSelection() && !config.shouldLoginPluginSupport()) {
                raceSelection.getSelectingPlayers().add(player.getUniqueId());
            }
        } else {
            bossBar.initBossBars(player, data.getRace());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        raceSelection.cancelTask(event.getPlayer().getUniqueId());
        raceSelection.getSelectingPlayers().remove(player.getUniqueId());
        bossBar.removeBossBars(uuid);
        plugin.saveData(uuid);
        playerDataMap.remove(uuid);
    }

    private void loginPluginSpecialSelection(Player player) {
        UUID uuid = player.getUniqueId();

        if (raceSelection.getSelectionTasks().containsKey(uuid)) {
            raceSelection.getSelectionTasks().get(uuid).cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() ||
                        playerDataMap.get(uuid).isHasChosen()) {
                    raceSelection.cancelTask(uuid);
                    return;
                }
                raceSelection.openRaceSelection(player);
            }
        }.runTaskTimer(plugin, 0, 5);

        raceSelection.getSelectionTasks().put(uuid, task);
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

    public void dragonEffects(Player player, PlayerDataManager data) {
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

    public void kirinEffects(Player player, PlayerDataManager data) {
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

    public void seaponyEffects(Player player, PlayerDataManager data) {
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
        }.runTaskLater(plugin, 60);

        lastBoostTime.put(player.getUniqueId(), now);

        bossBar.updateBossBars(player, data);
    }

    public void nightmareEffects(Player player, PlayerDataManager data) {
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

    public void pegasusEffects(Player player, PlayerDataManager data) {
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

    public void earthPonyEffects(Player player) {
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
        }.runTaskLater(plugin, 20);

        bossBar.updateBossBars(player, data);
    }

    public void unicornMana(Player player, PlayerDataManager data) {
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
        }.runTaskTimer(plugin, 0, 2);

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

                    raceSelection.openRaceSelection(player);

                    player.setMetadata("using_rebirth_potion", new FixedMetadataValue(plugin, true));
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
}