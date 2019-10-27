package de.zweistein2.plugins.zw2afk;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.zweistein2.plugins.zw2afk.util.PermissionsExHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static de.zweistein2.plugins.zw2afk.util.CommandHelper.*;

public class Zw2Afk extends JavaPlugin implements Listener
{
    private static final ArrayList<UUID> AFK_LIST = new ArrayList<>();
    private static final HashMap<Player, Long> LAST_ACTIVE = new HashMap<>();
    private static final HashMap<Player, Integer> TASK_LIST = new HashMap<>();
    private static final HashMap<Player, Integer> TIME_LIST = new HashMap<>();
    private static final double MOVEMENT_DISTANCE = 0.2;
    private static final long TIME_TILL_PLAYER_IS_CHECKED_AFK = 200L;
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
        this.getLogger().info("Plugin erfolgreich deaktiviert!");
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args)
    {
        if(cmd.getName().equalsIgnoreCase("afk"))
        {
            return handleAfkCommand(sender, args, scheduler, AFK_LIST, TASK_LIST, TIME_LIST, this);
        }
        if(cmd.getName().equalsIgnoreCase("re"))
        {
            return handleReCommand(sender, args, scheduler, AFK_LIST, TASK_LIST, TIME_LIST, this);
        }
        if(cmd.getName().equalsIgnoreCase("reload"))
        {
            return handleReloadCommand(sender, this);
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        final Player joiningPlayer = event.getPlayer();
        setPlayerAfk(joiningPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this);

        scheduler.scheduleSyncDelayedTask(this, () ->
        {
            if(!AFK_LIST.isEmpty())
            {
                for(int i = 0; i <= (AFK_LIST.size() - 1); i++)
                {
                    player = getServer().getPlayer(AFK_LIST.get(i));
                    if(player != null)
                    {
                        player.setPlayerListName("§7" + PermissionsExHelper.getPlayerListName(player).replaceAll("&((?i)[0-9a-fk-or])", ""));
                    }
                }
            }
        }, TIME_TILL_PLAYER_IS_CHECKED_AFK);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event)
    {
        final Player leavingPlayer = event.getPlayer();
        setPlayerBackAfterLeaveOrKick(leavingPlayer, scheduler, AFK_LIST, TASK_LIST, TIME_LIST);
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event)
    {
        final Player kickedPlayer = event.getPlayer();
        setPlayerBackAfterLeaveOrKick(kickedPlayer, scheduler, AFK_LIST, TASK_LIST, TIME_LIST);
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
            setPlayerAfk(movingPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this);
        }

        if(to != null && to.distance(from) > MOVEMENT_DISTANCE) {
            setPlayerBack(movingPlayer, scheduler, AFK_LIST, TASK_LIST, TIME_LIST, this);
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
            setPlayerAfk(chattingPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this);
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
            setPlayerAfk(interactingPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this);
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
            setPlayerAfk(fishingPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this);
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
}