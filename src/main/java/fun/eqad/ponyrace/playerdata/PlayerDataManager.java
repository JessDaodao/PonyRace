package fun.eqad.ponyrace.playerdata;

import com.google.gson.annotations.Expose;
import org.bukkit.Location;
import java.util.UUID;

public class PlayerDataManager {
    @Expose private final UUID uuid;
    @Expose private String race;
    @Expose private String lastWorld;
    @Expose private int stamina = 100;
    @Expose private int mana = 100;
    @Expose private int enrageTime = 0;
    @Expose private long lastFlightMoveTime;
    @Expose private Location lastFlightLocation;
    @Expose private boolean hasChosen = false;
    @Expose private boolean usingAbility = false;
    @Expose private boolean isEnraged = false;
    @Expose private boolean staminaExhausted = false;

    public PlayerDataManager(UUID uuid) { this.uuid = uuid; }

    public UUID getUuid() { return uuid; }
    public String getRace() { return race; }
    public String getLastWorld() { return lastWorld; }
    public int getStamina() { return stamina; }
    public int getMana() { return mana; }
    public int getEnrageTime() { return enrageTime; }
    public long getLastFlightMoveTime() { return lastFlightMoveTime; }
    public Location getLastFlightLocation() { return lastFlightLocation; }
    public boolean isEnraged() { return isEnraged && enrageTime > 0; }
    public boolean isHasChosen() { return hasChosen; }
    public boolean isUsingAbility() { return usingAbility; }
    public boolean isStaminaExhausted() { return staminaExhausted; }
    public void setLastFlightMoveTime(long lastFlightMoveTime) { this.lastFlightMoveTime = lastFlightMoveTime; }
    public void setLastFlightLocation(Location lastFlightLocation) { this.lastFlightLocation = lastFlightLocation; }
    public void setLastWorld(String worldName) { this.lastWorld = worldName; }
    public void setRace(String race) { this.race = race; }
    public void setStamina(int stamina) { this.stamina = Math.min(100, Math.max(0, stamina)); }
    public void setMana(int mana) { this.mana = Math.min(100, Math.max(0, mana)); }
    public void setEnrageTime(int time) { this.enrageTime = Math.min(100, Math.max(0, time)); }
    public void setEnraged(boolean state) { this.isEnraged = state; }
    public void setHasChosen(boolean hasChosen) { this.hasChosen = hasChosen; }
    public void setUsingAbility(boolean state) { this.usingAbility = state; }
    public void setStaminaExhausted(boolean staminaExhausted) { this.staminaExhausted = staminaExhausted; }

    public String getRaceName(String race) {
        if (race == null) return "无";

        switch (race.toLowerCase()) {
            case "pegasus": return "飞马";
            case "earthpony": return "陆马";
            case "unicorn": return "独角兽";
            case "nightmare": return "夜琪";
            case "seapony": return "海马";
            case "kirin": return "麒麟";
            case "dragon": return "龙族";
            case "human": return "人类";
            default: return race;
        }
    }
}