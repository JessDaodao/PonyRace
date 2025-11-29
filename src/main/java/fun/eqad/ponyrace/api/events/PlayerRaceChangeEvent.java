package fun.eqad.ponyrace.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerRaceChangeEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final String oldRace;
    private final String newRace;

    public PlayerRaceChangeEvent(Player player, String oldRace, String newRace) {
        super(player);
        this.oldRace = oldRace;
        this.newRace = newRace;
    }

    public String getOldRace() {
        return oldRace;
    }

    public String getNewRace() {
        return newRace;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}