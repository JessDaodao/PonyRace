package fun.eqad.ponyrace.config;

import fun.eqad.ponyrace.PonyRace;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String messagePrefix;
    private String aboutURL;
    private boolean showSelection;
    private boolean loginPluginSupport;
    private boolean forceSelection;
    private int manaLeftConsume;
    private int manaRightConsume;
    private int staminaConsume;
    private int staminaBoostConsume;
    private double staminaMoveConsume;
    private int staminaMoveMaxConsume;
    private int staminaMoveThresholdConsume;
    private int enrageConsume;
    private int manaRegen;
    private int staminaRegen;
    private int enrageRegen;
    private int manaCooldown;
    private int boostCooldown;
    private int eatCooldown;
    private int dragonFireCooldown;
    private int flyCooldown;
    private int enrageCooldown;
    private int maxTeleportLength;
    private int maxLaserLength;
    private int laserDamage;

    public ConfigManager(PonyRace plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void reload() {
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        messagePrefix = ChatColor.translateAlternateColorCodes('&', config.getString("messages.prefix", "&8[&bPonyRace&8]&r "));
        aboutURL = config.getString("messages.about-url", "https://www.eqad.fun/wiki/ponyrace");
        showSelection = config.getBoolean("settings.selection.show", true);
        loginPluginSupport = config.getBoolean("settings.selection.login-plugin-support", false);
        forceSelection = config.getBoolean("settings.selection.force", true);
        manaLeftConsume = config.getInt("settings.consume.mana-left", 20);
        manaRightConsume = config.getInt("settings.consume.mana-right", 50);
        staminaConsume = config.getInt("settings.consume.stamina", 1);
        staminaBoostConsume = config.getInt("settings.consume.stamina-boost", 20);
        staminaMoveConsume = config.getDouble("settings.consume.stamina-move", 0.5);
        staminaMoveMaxConsume = config.getInt("settings.consume.stamina-move-max", 3);
        staminaMoveThresholdConsume = config.getInt("settings.consume.stamina-move-threshold", 8);
        enrageConsume = config.getInt("settings.consume.enrage", 3);
        manaRegen = config.getInt("settings.regen.mana", 1);
        staminaRegen = config.getInt("settings.regen.stamina", 2);
        enrageRegen = config.getInt("settings.regen.enrage", 1);
        manaCooldown = config.getInt("settings.cooldown.mana", 1);
        boostCooldown = config.getInt("settings.cooldown.boost", 1);
        eatCooldown = config.getInt("settings.cooldown.eat", 1);
        dragonFireCooldown = config.getInt("settings.cooldown.dragon-fire", 30);
        flyCooldown = config.getInt("settings.cooldown.fly", 20);
        enrageCooldown = config.getInt("settings.cooldown.enrage", 20);
        maxTeleportLength = config.getInt("settings.skill.max-teleport-length", 100);
        maxLaserLength = config.getInt("settings.skill.max-laser-length", 60);
        laserDamage = config.getInt("settings.skill.laser-damage", 12);
    }

    public String getMessagePrefix() { return messagePrefix; }
    public String getAboutURL() { return aboutURL; }
    public boolean shouldShowSelection() { return showSelection; }
    public boolean shouldLoginPluginSupport() { return loginPluginSupport; }
    public boolean shouldForceSelection() { return forceSelection; }
    public int getManaLeftConsume() { return manaLeftConsume; }
    public int getManaRightConsume() { return manaRightConsume; }
    public int getStaminaConsume() { return staminaConsume; }
    public int getStaminaBoostConsume() { return staminaBoostConsume; }
    public double getStaminaMoveConsume() { return staminaMoveConsume; }
    public int getStaminaMoveMaxConsume() { return staminaMoveMaxConsume; }
    public int getStaminaMoveThresholdConsume() { return staminaMoveThresholdConsume; }
    public int getEnrageConsume() { return enrageConsume; }
    public int getManaRegen() { return manaRegen; }
    public int getStaminaRegen() { return staminaRegen; }
    public int getEnrageRegen() { return enrageRegen; }
    public int getManaCooldown() { return manaCooldown * 1000; }
    public int getBoostCooldown() { return boostCooldown * 1000; }
    public int getEatCooldown() { return eatCooldown * 1000; }
    public int getDragonFireCooldown() { return dragonFireCooldown * 1000; }
    public int getFlyCooldown() { return flyCooldown; }
    public int getEnrageCooldown() { return enrageCooldown; }
    public int getMaxTeleportLength() { return maxTeleportLength; }
    public int getMaxLaserLength() { return maxLaserLength; }
    public int getLaserDamage() { return laserDamage; }
}