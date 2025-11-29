package fun.eqad.ponyrace.api;

import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public interface PonyRaceAPI {
    /**
     * 获取插件实例
     * @return PonyRace插件实例
     */
    Plugin getPlugin();

    /**
     * 获取玩家的种族数据
     * @param uuid 玩家UUID
     * @return 玩家数据管理器，如果玩家不存在则返回null
     */
    PlayerDataManager getPlayerData(UUID uuid);

    /**
     * 获取玩家的种族
     * @param player 玩家
     * @return 种族ID，如果玩家未选择种族则返回null
     */
    String getPlayerRace(Player player);

    /**
     * 获取玩家的体力值
     * @param player 玩家
     * @return 体力值(0-100)
     */
    int getPlayerStamina(Player player);

    /**
     * 获取玩家的魔力值
     * @param player 玩家
     * @return 魔力值(0-100)
     */
    int getPlayerMana(Player player);

    /**
     * 获取玩家的魔力值
     * @param player 玩家
     * @return 魔力值(0-100)
     */
    int getPlayerEnrage(Player player);

    /**
     * 检查玩家是否拥有特定种族
     * @param player 玩家
     * @param race 要检查的种族
     * @return 如果玩家拥有特定种族则返回true
     */
    boolean hasRace(Player player, String race);

    /**
     * 检查玩家是否已选择种族
     * @param player 玩家
     * @return 如果已选择种族则返回true
     */
    boolean hasChosenRace(Player player);

    /**
     * 设置玩家的种族
     * @param player 玩家
     * @param race 种族名称
     * @return 如果设置成功则返回true
     */
    boolean setPlayerRace(Player player, String race);

    /**
     * 设置玩家的魔力值
     * @param player 玩家
     * @param value 魔力值(0-100)
     * @return 如果设置成功则返回true
     */
    boolean setPlayerMana(Player player, int value);

    /**
     * 设置玩家的体力值
     * @param player 玩家
     * @param value 体力值(0-100)
     * @return 如果设置成功则返回true
     */
    boolean setPlayerStamina(Player player, int value);

    /**
     * 设置玩家的怒气值
     * @param player 玩家
     * @param value 怒气值(0-100)
     * @return 如果设置成功则返回true
     */
    boolean setPlayerEnrage(Player player, int value);
}