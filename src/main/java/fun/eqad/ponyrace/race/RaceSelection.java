package fun.eqad.ponyrace.race;

import fun.eqad.ponyrace.PonyRace;
import fun.eqad.ponyrace.api.events.PlayerRaceChangeEvent;
import fun.eqad.ponyrace.bossbar.BossBarManager;
import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RaceSelection implements Listener {
    private final PonyRace plugin;
    private final Set<UUID> selectingPlayers;
    private final Map<UUID, BukkitTask> selectionTasks;

    public RaceSelection(PonyRace plugin) {
        this.plugin = plugin;
        this.selectingPlayers = new HashSet<>();
        this.selectionTasks = new HashMap<>();
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
                player.removeMetadata("using_rebirth_potion", plugin);

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
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (selectingPlayers.contains(player.getUniqueId())) {
                    openRaceSelection(player);
                }
            }, 1L);
        }
    }

    public void changeRace(Player player, String race) {
        PlayerDataManager data = plugin.getPlayerData(player.getUniqueId());
        String oldRace = data != null ? data.getRace() : null;

        PlayerRaceChangeEvent event = new PlayerRaceChangeEvent(player, oldRace, race);
        Bukkit.getPluginManager().callEvent(event);

        if (data == null) {
            data = new PlayerDataManager(player.getUniqueId());
            plugin.getPlayerDataMap().put(player.getUniqueId(), data);
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
        PlayerDataManager data = plugin.getPlayerData(player.getUniqueId());
        BossBarManager bossBar = plugin.getBossBarManager();
        
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
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§b你已转生为飞马!");
                break;
            case "earthpony":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§6你已转生为陆马!");
                break;
            case "unicorn":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§d你已转生为独角兽!");
                break;
            case "nightmare":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§2你已转生为夜琪!");
                break;
            case "seapony":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§3你已转生为海马!");
                break;
            case "kirin":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§e你已转生为麒麟!");
                break;
            case "dragon":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§4你已转生为龙族!");
                break;
            case "human":
                player.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§7你已转生为人类!");
                break;
        }

        plugin.saveData(player.getUniqueId());
    }

    public void cancelTask(UUID uuid) {
        if (selectionTasks.containsKey(uuid)) {
            selectionTasks.get(uuid).cancel();
            selectionTasks.remove(uuid);
        }
    }

    public Set<UUID> getSelectingPlayers() {
        return selectingPlayers;
    }

    public Map<UUID, BukkitTask> getSelectionTasks() {
        return selectionTasks;
    }
}