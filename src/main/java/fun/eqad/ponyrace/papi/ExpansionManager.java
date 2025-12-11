package fun.eqad.ponyrace.papi;

import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import fun.eqad.ponyrace.PonyRace;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class ExpansionManager extends PlaceholderExpansion {
    private final PonyRace plugin;

    public ExpansionManager(PonyRace plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "ponyrace"; }
    @Override public String getAuthor() { return "EQAD Network"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) return "";

        PlayerDataManager data = plugin.getPlayerDataMap().get(player.getUniqueId());
        if (data == null) return "";

        switch (params.toLowerCase()) {
            case "race": return data.getRaceName(data.getRace());
            case "stamina": return String.valueOf(data.getStamina());
            case "mana": return String.valueOf(data.getMana());
            case "enrage_time": return String.valueOf(data.getEnrageTime());
            default: return null;
        }
    }
}