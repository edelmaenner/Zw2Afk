package de.zweistein2.util;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import de.zweistein2.Zw2Afk;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandHelper
{

    public static final String KEINE_BERECHTIGUNG = "Du hast keine Berechtigung um diesen Befehl zu benutzen";

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

        if(sendingPlayer.hasPermission("Zw2Afk.afk"))
        {
            return setPlayerAfkAfterCommand(sender, args, sendingPlayer, scheduler, afkList, taskList, timeList,
                                            instance);
        } else {
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
            if(sendingPlayer.hasPermission("Zw2Afk.re"))
            {
                return setPlayerBackAfterCommand(sendingPlayer, scheduler, afkList, taskList, timeList, instance);
            } else {
                sendingPlayer.sendMessage(KEINE_BERECHTIGUNG);
            }
        }

        return false;
    }

    public static boolean handleReloadCommand(final CommandSender sender, final Zw2Afk instance)
    {
        if((sender instanceof Player && sender.hasPermission("Zw2Afk.reload")) || (sender instanceof ConsoleCommandSender))
        {
            instance.reloadConfig();
            instance.getLogger().info("Die Config wurde neu geladen!");
            sender.sendMessage("Die Config wurde neu geladen!");
            return true;
        } else {
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
            sendingPlayer.setPlayerListName("§f" + sendingPlayer.getName());
            sendingPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, timeList.get(sendingPlayer));
            sendingPlayer.setSleepingIgnored(false);
        } else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist gar nicht afk!");
            return false;
        }
        return true;
    }

    private static boolean setPlayerAfkAfterCommand(final CommandSender sender, final String[] args,
                                                    final Player sendingPlayer, final BukkitScheduler scheduler,
                                                    final List<UUID> afkList, final Map<Player, Integer> taskList,
                                                    final Map<Player, Integer> timeList, final Zw2Afk instance)
    {
        if(!afkList.contains(sendingPlayer.getUniqueId()))
        {
            if(!taskList.isEmpty())
            {
                scheduler.cancelTask(taskList.get(sendingPlayer));
            }
            if(args.length == 1)
            {
                instance.getServer()
                        .broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk: " + args[0]);
            } else if(args.length > 1)
            {
                final StringBuilder line1 = new StringBuilder();
                for(final String arg : args)
                {
                    line1.append(arg).append(" ");
                }
                instance.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk: " + line1);
            } else
            {
                instance.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk");
            }
            afkList.add(sendingPlayer.getUniqueId());
            sendingPlayer.setPlayerListName("§7" + sender.getName());
            timeList.put(sendingPlayer, sendingPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE));
            taskList.put(sendingPlayer, scheduler
                    .scheduleSyncDelayedTask(instance, () -> sendingPlayer.setSleepingIgnored(true), 1200L));
        } else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist schon afk!");
            return false;
        }
        return true;
    }

    public static void setPlayerBack(final Player kickedPlayer, final BukkitScheduler scheduler,
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
                player.setPlayerListName("§7" + player.getName());
                timeList.put(player, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
                player.setSleepingIgnored(true);
            }
        }, 18000L));
    }
}
