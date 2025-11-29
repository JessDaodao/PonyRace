package fun.eqad.ponyrace.command;

import fun.eqad.ponyrace.playerdata.PlayerDataManager;
import fun.eqad.ponyrace.PonyRace;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final PonyRace plugin;
    private final String[] races = {"pegasus", "earthpony", "unicorn", "nightmare", "seapony", "kirin", "dragon", "human"};
    private final String[] potionItems = {"rebirth_potion", "mana_potion", "stamina_potion"};

    public CommandManager(PonyRace plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            help(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                set(sender, args, label);
                break;
            case "info":
                info(sender, args);
                break;
            case "gui":
                gui(sender, args);
                break;
            case "give":
                give(sender, args, label);
                break;
            case "reload":
                reload(sender);
                break;
            case "about":
                about(sender);
                break;
            default:
                help(sender, label);
        }

        return true;
    }

    public void sendHyperlink(CommandSender sender, String displayText, String url, String hoverText) {
        TextComponent message = new TextComponent(displayText);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()));
        sender.spigot().sendMessage(message);
    }

    private Player targetPlayerArg2(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player) {
                return (Player) sender;
            }
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c控制台使用时必须指定玩家");
            return null;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c该玩家不存在");
            return null;
        }
        return target;
    }

    private Player targetPlayerArg3(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            Player player = plugin.getServer().getPlayer(args[2]);
            if (player == null) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c该玩家不存在");
                return null;
            }
            return player;
        }

        if (sender instanceof Player) {
            return (Player) sender;
        }

        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c控制台使用时必须指定玩家");
        return null;
    }

    public boolean isValidRace(String race) {
        for (String valid : races) {
            if (valid.equalsIgnoreCase(race)) return true;
        }

        return false;
    }

    private void help(CommandSender sender, String label) {
        boolean aliases = label.equalsIgnoreCase("pr");

        if (aliases) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§7PonyRace帮助:");
            sender.sendMessage(" §7/pr set <种族> [玩家] §8- §7设置种族");
            sender.sendMessage(" §7/pr info [玩家] §8- §7查看种族信息");
            sender.sendMessage(" §7/pr gui [玩家] §8- §7打开种族选择菜单");
            sender.sendMessage(" §7/pr give <物品> [玩家/数量] [数量] §8- §7获取插件物品");
            sender.sendMessage(" §7/pr reload §8- §7重载配置文件");
            sender.sendMessage(" §7/pr about §8- §7关于PonyRace");
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§7PonyRace帮助:");
            sender.sendMessage(" §7/ponyrace set <种族> [玩家] §8- §7设置种族");
            sender.sendMessage(" §7/ponyrace info [玩家] §8- §7查看种族信息");
            sender.sendMessage(" §7/ponyrace gui [玩家] §8- §7打开种族选择菜单");
            sender.sendMessage(" §7/ponyrace give <物品> [玩家/数量] [数量] §8- §7获取插件物品");
            sender.sendMessage(" §7/ponyrace reload §8- §7重载配置文件");
            sender.sendMessage(" §7/ponyrace about §8- §7关于PonyRace");
        }
    }

    private void set(CommandSender sender, String[] args, String label) {
        boolean aliases = label.equalsIgnoreCase("pr");

        if (!sender.hasPermission("ponyrace.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c你没有执行该命令的权限");
            return;
        }

        if (args.length < 2) {
            if (aliases) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c用法: /pr set <种族> [玩家]");
                return;
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c用法: /ponyrace set <种族> [玩家]");
                return;
            }
        }

        String race = args[1].toLowerCase();
        if (!isValidRace(race)) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c无效的种族, 可选: \n pegasus, earthpony, unicorn, nightmare, seapony, kirin, dragon, human");
            return;
        }

        Player target = targetPlayerArg3(sender, args);
        if (target == null) return;

        plugin.changeRace(target, race);
        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§a成功将 " + target.getName() + " 的种族设置为 " + race);
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length >= 2 && !sender.hasPermission("ponyrace.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c你没有执行该命令的权限");
            return;
        }

        Player target = targetPlayerArg2(sender, args);
        if (target == null) return;

        PlayerDataManager data = plugin.getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c该玩家的数据为空");
            return;
        }

        String raceID = data.getRace() != null ? data.getRace() : "无";
        String message = String.join("\n",
                plugin.getConfigManager().getMessagePrefix() + "§7玩家种族信息:",
                " §7名称 §8- §7" + target.getName(),
                " §7种族 §8- §7" + data.getRaceName(data.getRace()),
                " §7体力 §8- §7" + data.getStamina() + "/100",
                " §7魔力 §8- §7" + data.getMana() + "/100",
                " §7怒气 §8- §7" + data.getEnrageTime() + "/100"
        );

        sender.sendMessage(message);
    }

    private void gui(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ponyrace.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c你没有执行该命令的权限");
            return;
        }

        Player target = null;
        if (args.length >= 2) {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c该玩家不存在");
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c控制台使用时必须指定玩家");
            return;
        }

        plugin.openRaceSelection(target);
        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§a已为 " + target.getName() + " 打开种族选择菜单");
    }

    private void give(CommandSender sender, String[] args, String label) {
        boolean aliases = label.equalsIgnoreCase("pr");

        if (!sender.hasPermission("ponyrace.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c你没有执行该命令的权限");
            return;
        }

        if (args.length < 2) {
            if (aliases) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c用法: /pr give <物品> [玩家/数量] [数量]");
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c用法: /ponyrace give <物品> [玩家/数量] [数量]");
            }
            return;
        }

        String itemName = args[1].toLowerCase();
        ItemStack item = null;

        switch (itemName) {
            case "rebirth_potion":
                item = plugin.getRecipeManager().createRebirthPotion();
                break;
            case "mana_potion":
                item = plugin.getRecipeManager().createManaPotion();
                break;
            case "stamina_potion":
                item = plugin.getRecipeManager().createStaminaPotion();
                break;
            default:
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c无效的物品名称, 可选: \n rebirth_potion, mana_potion, stamina_potion");
                return;
        }

        Player target;
        int amountArgIndex = 2;

        if (args.length > 2 && !isNumeric(args[2])) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c该玩家不存在");
                return;
            }
            amountArgIndex = 3;
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c控制台使用时必须指定玩家");
            return;
        }

        int amount = 1;
        if (args.length > amountArgIndex) {
            try {
                amount = Integer.parseInt(args[amountArgIndex]);
                amount = Math.max(1, Math.min(64, amount));
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c数量必须是数字");
                return;
            }
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§a已给予 " + target.getName() + " " + amount + " 个 " + itemName);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("ponyrace.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§c你没有执行该命令的权限");
            return;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§a配置重载成功");
    }

    private void about(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessagePrefix() + "§7关于PonyRace:");
        sender.sendMessage(" §7一个让玩家扮演MLP种族的插件");
        sendHyperlink(sender, " §7[查看使用文档]", plugin.getConfigManager().getAboutURL(), "§7点击打开链接");
        sender.sendMessage(" §7版本 §8- §7" + plugin.getDescription().getVersion());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if ("set".startsWith(args[0].toLowerCase())) commands.add("set");
            if ("info".startsWith(args[0].toLowerCase())) commands.add("info");
            if ("gui".startsWith(args[0].toLowerCase())) commands.add("gui");
            if ("give".startsWith(args[0].toLowerCase())) commands.add("give");
            if ("reload".startsWith(args[0].toLowerCase())) commands.add("reload");
            if ("about".startsWith(args[0].toLowerCase())) commands.add("about");
            return commands;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (String race : races) {
                if (StringUtil.startsWithIgnoreCase(race, args[1])) {
                    completions.add(race);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(player.getName(), args[2])) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(p.getName(), args[1])) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(p.getName(), args[1])) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (String potion : potionItems) {
                if (StringUtil.startsWithIgnoreCase(potion, args[1])) {
                    completions.add(potion);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (StringUtil.startsWithIgnoreCase(p.getName(), args[2])) {
                    completions.add(p.getName());
                }
            }
            completions.add("[数量]");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            if (!isNumeric(args[2])) {
                completions.add("[数量]");
            }
        }

        Collections.sort(completions);
        return completions;
    }
}