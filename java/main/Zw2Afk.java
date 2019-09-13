package main;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import de.myzelyam.api.vanish.*;

public class Zw2Afk extends JavaPlugin implements Listener
{
    private static final ArrayList<UUID> AFK_LIST = new ArrayList<>();
    private static final HashMap<Player,Long> LAST_ACTIVE = new HashMap<>();
    private static final HashMap<Player,Integer> TASK_LIST = new HashMap<>();
    private static final HashMap<Player,Integer> TIME_LIST = new HashMap<>();
    private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    private Player player;
    
    @Override
    public void onLoad() 
    {
        //
    }
    
    @Override
    public void onEnable()
    {
        this.getLogger().info("Plugin erfolgreich geladen!");
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("Listener erfolgreich geladen!");
    }
    
    @Override
    public void onDisable()
    {
        scheduler.cancelTasks(this);
        this.getLogger().info("[Zw2Afk] Plugin erfolgreich deaktiviert!");
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args)
    {    
        if(cmd.getName().equalsIgnoreCase("afk"))
        {
            return handleAfkCommand(sender, args);
        }
        if(cmd.getName().equalsIgnoreCase("re"))
        {
            return handleReCommand(sender, args);
        }
        if(cmd.getName().equalsIgnoreCase("reload"))
        {
            return handleReloadCommand(sender);
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        final Player joiningPlayer = event.getPlayer();
        setPlayerAfk(joiningPlayer);

        scheduler.scheduleSyncDelayedTask(this, () -> {
            if(!AFK_LIST.isEmpty())
            {
                for(int i = 0; i <= (AFK_LIST.size()-1); i++)
                {
                    player = getServer().getPlayer(AFK_LIST.get(i));
                    if (player != null) {
                        player.setPlayerListName("§7"+ player.getName());
                    }
                }
            }
        }, 200L);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        final Player leavingPlayer = event.getPlayer();
        setPlayerBack(leavingPlayer);
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event)
    {
        final Player kickedPlayer = event.getPlayer();
        setPlayerBack(kickedPlayer);
    }

    @EventHandler
    public void onPlayerMoveEvent(final PlayerMoveEvent event)
    {
        final Player movingPlayer = event.getPlayer();
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(movingPlayer));
        }
        
        final Location from = event.getFrom();
        final Location to = event.getTo();
        if(!AFK_LIST.contains(movingPlayer.getUniqueId()))
        {
            setPlayerAfk(movingPlayer);
        }
        if(AFK_LIST.contains(movingPlayer.getUniqueId()) && to != null && to.distance(from) > 0.2)
        {
            AFK_LIST.remove(movingPlayer.getUniqueId());
            this.getServer().broadcastMessage(ChatColor.GOLD + movingPlayer.getName() + " ist wieder da");
            movingPlayer.setPlayerListName("§f"+movingPlayer.getName());
            movingPlayer.setStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE, TIME_LIST.get(movingPlayer));
            movingPlayer.setSleepingIgnored(false);
        }
    }
    
    @EventHandler
    public void onAsyncPlayerChatEvent(final AsyncPlayerChatEvent event)
    {
        final Player chattingPlayer = event.getPlayer();
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(chattingPlayer));
        }
        if(!AFK_LIST.contains(chattingPlayer.getUniqueId()))
        {
            setPlayerAfk(chattingPlayer);
        }
    }
    
    @EventHandler
    public void onPlayerInteractEvent(final PlayerInteractEvent event)
    {
        final Player interactingPlayer = event.getPlayer();
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(interactingPlayer));
        }
        if(!AFK_LIST.contains(interactingPlayer.getUniqueId()))
        {
            setPlayerAfk(interactingPlayer);
        }
    }
    
    @EventHandler
    public void onPlayerFishEvent(final PlayerFishEvent event)
    {
        final Player fishingPlayer = event.getPlayer();
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(fishingPlayer));
        }
        if(!AFK_LIST.contains(fishingPlayer.getUniqueId()))
        {
            setPlayerAfk(fishingPlayer);
        }
    }

    @EventHandler
    public void onVanish(final PlayerHideEvent event)
    {
        final Player vanshingPlayer = event.getPlayer();
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(vanshingPlayer));
        }
    }

    private boolean handleAfkCommand(final CommandSender sender, final String[] args) {
        if(!(sender instanceof Player))
        {
            this.getLogger().info("Dieser Befehl ist nur für Spieler!");
            return true;
        }

        final Player sendingPlayer = (Player) sender;

        if(sendingPlayer.hasPermission("Zw2Afk.afk"))
        {
            return setPlayerAfkAfterCommand(sender, args, sendingPlayer);
        }

        return false;
    }

    private boolean handleReCommand(final CommandSender sender, final String[] args) {
        if(args.length == 0) {
            if(!(sender instanceof Player))
            {
                this.getLogger().info("Dieser Befehl ist nur für Spieler!");
                return true;
            }
            final Player sendingPlayer = (Player) sender;
            if(sendingPlayer.hasPermission("Zw2Afk.re"))
            {
                return setPlayerBackAfterCommand(sendingPlayer);
            }
        }

        return false;
    }

    private boolean handleReloadCommand(final CommandSender sender) {
        if(sender.hasPermission("Zw2Afk.reload"))
        {
            this.reloadConfig();
            return true;
        }

        return false;
    }

    private boolean setPlayerBackAfterCommand(final Player sendingPlayer) {
        if(!TASK_LIST.isEmpty())
        {
            scheduler.cancelTask(TASK_LIST.get(sendingPlayer));
        }
        if(AFK_LIST.contains(sendingPlayer.getUniqueId()))
        {
            AFK_LIST.remove(sendingPlayer.getUniqueId());
            this.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist wieder da");
            sendingPlayer.setPlayerListName("§f"+sendingPlayer.getName());
            sendingPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, TIME_LIST.get(sendingPlayer));
            sendingPlayer.setSleepingIgnored(false);
        }else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist gar nicht afk!");
            return false;
        }
        return true;
    }

    private boolean setPlayerAfkAfterCommand(final CommandSender sender, final String[] args, final Player sendingPlayer) {
        if(!AFK_LIST.contains(sendingPlayer.getUniqueId()))
        {
            if(!TASK_LIST.isEmpty())
            {
                scheduler.cancelTask(TASK_LIST.get(sendingPlayer));
            }
            if(args.length == 1)
            {
                this.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk: " + args[0]);
            }else
            if(args.length > 1)
            {
                final StringBuilder line1 = new StringBuilder();
                for (final String arg : args) {
                    line1.append(arg).append(" ");
                }
                this.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk: " + line1);
            }else
            {
                this.getServer().broadcastMessage(ChatColor.GOLD + sendingPlayer.getName() + " ist afk");
            }
            AFK_LIST.add(sendingPlayer.getUniqueId());
            sendingPlayer.setPlayerListName("§7"+sender.getName());
            TIME_LIST.put(sendingPlayer, sendingPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE));
            TASK_LIST.put(sendingPlayer, scheduler.scheduleSyncDelayedTask(this, () -> sendingPlayer.setSleepingIgnored(true), 1200L));
        }else
        {
            sendingPlayer.sendMessage(ChatColor.RED + "Du bist schon afk!");
            return false;
        }
        return true;
    }

    private void setPlayerBack(final Player kickedPlayer) {
        if (!TASK_LIST.isEmpty()) {
            scheduler.cancelTask(TASK_LIST.get(kickedPlayer));
        }
        if (AFK_LIST.contains(kickedPlayer.getUniqueId())) {
            AFK_LIST.remove(kickedPlayer.getUniqueId());
            kickedPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, TIME_LIST.get(kickedPlayer));
            kickedPlayer.setSleepingIgnored(false);
        }
    }

    private void setPlayerAfk(final Player player) {
        LAST_ACTIVE.put(player, System.currentTimeMillis());

        TASK_LIST.put(player, scheduler.scheduleSyncDelayedTask(this, () -> {
            if(LAST_ACTIVE.get(player) != System.currentTimeMillis())
            {
                getServer().broadcastMessage(ChatColor.GOLD + player.getName() + " ist afk");
                AFK_LIST.add(player.getUniqueId());
                player.setPlayerListName("§7"+player.getName());
                TIME_LIST.put(player, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
                player.setSleepingIgnored(true);
            }
        }, 18000L));
    }
}