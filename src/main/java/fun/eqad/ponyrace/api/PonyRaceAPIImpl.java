package fun.eqad.ponyrace.api;

import fun.eqad.ponyrace.PonyRace;
import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public class PonyRaceAPIImpl implements PonyRaceAPI {
    private final PonyRace plugin;

    public PonyRaceAPIImpl(PonyRace plugin) {
        this.plugin = plugin;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public PlayerDataManager getPlayerData(UUID uuid) {
        return plugin.getPlayerData(uuid);
    }

    @Override
    public String getPlayerRace(Player player) {
        PlayerDataManager data = plugin.getPlayerData(player.getUniqueId());
        return data != null ? data.getRace() : null;
    }

    @Override
    public int getPlayerStamina(Player player) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        return data != null ? data.getStamina() : 0;
    }

    @Override
    public int getPlayerMana(Player player) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        return data != null ? data.getMana() : 0;
    }

    @Override
    public int getPlayerEnrage(Player player) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        return data != null ? data.getEnrageTime() : 0;
    }

    @Override
    public boolean hasRace(Player player, String race) {
        PlayerDataManager data = plugin.getPlayerData(player.getUniqueId());
        return data != null && race.equalsIgnoreCase(data.getRace());
    }

    @Override
    public boolean hasChosenRace(Player player) {
        PlayerDataManager data = plugin.getPlayerData(player.getUniqueId());
        return data != null && data.isHasChosen();
    }

    @Override
    public boolean setPlayerRace(Player player, String race) {
        if (player == null || race == null) return false;

        plugin.getRaceSelection().changeRace(player, race.toLowerCase());
        return true;
    }

    @Override
    public boolean setPlayerMana(Player player, int value) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        if (data == null) return false;

        value = Math.max(0, Math.min(100, value));
        data.setMana(value);

        plugin.getBossBarManager().updateBossBars(player, data);
        return true;
    }

    @Override
    public boolean setPlayerStamina(Player player, int value) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        if (data == null) return false;

        value = Math.max(0, Math.min(100, value));
        data.setStamina(value);

        plugin.getBossBarManager().updateBossBars(player, data);
        return true;
    }

    @Override
    public boolean setPlayerEnrage(Player player, int value) {
        PlayerDataManager data = getPlayerData(player.getUniqueId());
        if (data == null) return false;

        value = Math.max(0, Math.min(100, value));
        data.setEnrageTime(value);

        plugin.getBossBarManager().kirinRageBar(player, data);
        return true;
    }
}