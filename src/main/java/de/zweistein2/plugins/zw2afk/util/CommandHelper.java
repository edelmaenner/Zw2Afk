package de.zweistein2.plugins.zw2afk.util;

import de.zweistein2.plugins.zw2afk.Zw2Afk;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandHelper
{

    private static final String KEINE_BERECHTIGUNG = "Du hast keine Berechtigung um diesen Befehl zu benutzen";
    private static final long TIME_TILL_PLAYER_IS_IGNORED_BY_SLEEP_CHECK = 1200L;
    private static final long TIME_TILL_PLAYER_IS_DECLARED_AFK = 18000L;
    private static final String COLOR_CODECS = "&((?i)[0-9a-fk-or])";

    private CommandHelper()
    {
    }

    public static boolean handleAfkCommand(final CommandSender sender, final String[] args,
                                           final BukkitScheduler scheduler, final List<UUID> afkList,
                                           final Map<Player, Integer> taskList, final Map<Player, Integer> timeList,
                                           final Zw2Afk instance)
    {
        if(!(sender instanceof Player))
        {
            instance.getLogger().info("Dieser Befehl ist nur für Spieler!");
            return true;
        }

        final Player sendingPlayer = (Player) sender;

        if(sendingPlayer.hasPermission("zw2afk.afk"))
        {
            return setPlayerAfkAfterCommand(args, sendingPlayer, scheduler, afkList, taskList, timeList,
                                            instance);
        } else
        {
            sendingPlayer.sendMessage(KEINE_BERECHTIGUNG);
        }

        return false;
    }

    public static boolean handleReCommand(final CommandSender sender, final String[] args,
                                          final BukkitScheduler scheduler, final List<UUID> afkList,
                                          final Map<Player, Integer> taskList, final Map<Player, Integer> timeList,
                                          final Zw2Afk instance)
    {
        if(args.length == 0)
        {
            if(!(sender instanceof Player))
            {
                instance.getLogger().info("Dieser Befehl ist nur für Spieler!");
                return true;
            }
            final Player sendingPlayer = (Player) sender;
            if(sendingPlayer.hasPermission("zw2afk.re"))
            {
                return setPlayerBackAfterCommand(sendingPlayer, scheduler, afkList, taskList, timeList, instance);
            } else
            {
                sendingPlayer.sendMessage(KEINE_BERECHTIGUNG);
            }
        }

        return false;
    }

    public static boolean handleReloadCommand(final CommandSender sender, final Zw2Afk instance)
    {
        if((sender instanceof Player && sender.hasPermission("zw2afk.reload")) ||
           (sender instanceof ConsoleCommandSender))
        {
            instance.reloadConfig();
            instance.getLogger().info("Die Config wurde neu geladen!");
            sender.sendMessage("Die Config wurde neu geladen!");
            return true;
        } else
        {
            sender.sendMessage(KEINE_BERECHTIGUNG);
        }

        return false;
    }

    private static boolean setPlayerBackAfterCommand(final Player sendingPlayer, final BukkitScheduler scheduler,
                                                     final List<UUID> afkList, final Map<Player, Integer> taskList,
                                                     final Map<Player, Integer> timeList, final Zw2Afk instance)
    {
        if(!taskList.isEmpty())
        {
            scheduler.cancelTask(taskList.get(sendingPlayer));
        }
        if(afkList.contains(sendingPlayer.getUniqueId()))
        {
            afkList.remove(sendingPlayer.getUniqueId());
            instance.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist wieder da");
            sendingPlayer.setPlayerListName(PermissionsExHelper.getPlayerListName(sendingPlayer).replaceAll(COLOR_CODECS, "§$1"));
            sendingPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, timeList.get(sendingPlayer));
            sendingPlayer.setSleepingIgnored(false);
        } else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist gar nicht afk!");
            return false;
        }
        return true;
    }

    public static void setPlayerBack(final Player sendingPlayer, final BukkitScheduler scheduler,
                                         final List<UUID> afkList, final Map<Player, Integer> taskList,
                                         final Map<Player, Integer> timeList, final Zw2Afk instance)
    {
        if(!taskList.isEmpty())
        {
            scheduler.cancelTask(taskList.get(sendingPlayer));
        }
        if(afkList.contains(sendingPlayer.getUniqueId()))
        {
            afkList.remove(sendingPlayer.getUniqueId());
            instance.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist wieder da");
            sendingPlayer.setPlayerListName(PermissionsExHelper.getPlayerListName(sendingPlayer).replaceAll(
                    COLOR_CODECS, "§$1"));
            sendingPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, timeList.get(sendingPlayer));
            sendingPlayer.setSleepingIgnored(false);
        }
    }

    private static boolean setPlayerAfkAfterCommand(final String[] args, final Player sendingPlayer, final BukkitScheduler scheduler,
                                                    final List<UUID> afkList, final Map<Player, Integer> taskList,
                                                    final Map<Player, Integer> timeList, final Zw2Afk instance)
    {
        if(!afkList.contains(sendingPlayer.getUniqueId()))
        {
            if(!taskList.isEmpty())
            {
                scheduler.cancelTask(taskList.get(sendingPlayer));
            }
            instance.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk" +
                                                  (args.length == 0 ? "" : (": " + String.join(" ", args))));
            afkList.add(sendingPlayer.getUniqueId());
            sendingPlayer.setPlayerListName("§7" + PermissionsExHelper.getPlayerListName(sendingPlayer).replaceAll(
                    COLOR_CODECS, ""));
            timeList.put(sendingPlayer, sendingPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE));
            taskList.put(sendingPlayer, scheduler
                    .scheduleSyncDelayedTask(instance, () -> sendingPlayer.setSleepingIgnored(true), TIME_TILL_PLAYER_IS_IGNORED_BY_SLEEP_CHECK));
        } else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist schon afk!");
            return false;
        }
        return true;
    }

    public static void setPlayerBackAfterLeaveOrKick(final Player kickedPlayer, final BukkitScheduler scheduler,
                                                     final List<UUID> afkList, final Map<Player, Integer> taskList,
                                                     final Map<Player, Integer> timeList)
    {
        if(!taskList.isEmpty())
        {
            scheduler.cancelTask(taskList.get(kickedPlayer));
        }
        if(afkList.contains(kickedPlayer.getUniqueId()))
        {
            afkList.remove(kickedPlayer.getUniqueId());
            kickedPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, timeList.get(kickedPlayer));
            kickedPlayer.setSleepingIgnored(false);
        }
    }

    public static void setPlayerAfk(final Player player, final BukkitScheduler scheduler, final List<UUID> afkList,
                                    final Map<Player, Long> lastActive, final Map<Player, Integer> taskList,
                                    final Map<Player, Integer> timeList, final Zw2Afk instance)
    {
        lastActive.put(player, System.currentTimeMillis());

        taskList.put(player, scheduler.scheduleSyncDelayedTask(instance, () ->
        {
            if(lastActive.get(player) != System.currentTimeMillis())
            {
                instance.getServer().broadcastMessage(ChatColor.GOLD + player.getName() + " ist afk");
                afkList.add(player.getUniqueId());
                player.setPlayerListName("§7" + PermissionsExHelper.getPlayerListName(player).replaceAll(COLOR_CODECS, ""));
                timeList.put(player, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
                player.setSleepingIgnored(true);
            }
        }, TIME_TILL_PLAYER_IS_DECLARED_AFK));
    }
}
