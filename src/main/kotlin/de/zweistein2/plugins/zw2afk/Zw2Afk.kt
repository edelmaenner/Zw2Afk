package de.zweistein2.plugins.zw2afk

import de.zweistein2.plugins.zw2afk.util.CommandHelper
import de.zweistein2.plugins.zw2afk.util.CommandHelper.getPlayerListName
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Zw2Afk : JavaPlugin(), Listener {
    companion object {
        val AFK_LIST: MutableList<UUID> = mutableListOf()
        private val LAST_ACTIVE: MutableMap<Player, Long> = mutableMapOf()
        private val TASK_LIST: MutableMap<Player, Int> = mutableMapOf()
        private val TIME_LIST: MutableMap<Player, Int> = mutableMapOf()
        val SUPPORTED_PLUGINS: MutableMap<String, Boolean> = mutableMapOf()
        private const val MOVEMENT_DISTANCE = 0.2
        private const val TIME_TILL_PLAYER_IS_CHECKED_AFK = 200L
    }

    private val scheduler = Bukkit.getServer().scheduler

    override fun onEnable() {
        logger.info("Loading plugin...")
        server.pluginManager.registerEvents(this, this)
        SUPPORTED_PLUGINS["DiscordSRV"] = server.pluginManager.isPluginEnabled("DiscordSRV")
        SUPPORTED_PLUGINS["PermissionsEx"] = server.pluginManager.isPluginEnabled("PermissionsEx")
        SUPPORTED_PLUGINS.filter { it.value }.forEach { logger.info("Enabled support for " + it.key) }
        logger.info("Done loading!")
    }

    override fun onDisable() {
        scheduler.cancelTasks(this)
        AFK_LIST.clear()
        LAST_ACTIVE.clear()
        TASK_LIST.clear()
        TIME_LIST.clear()
        logger.info("Plugin stopped!")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (command.name.equals("afk", ignoreCase = true)) {
            return CommandHelper.handleAfkCommand(sender, args, scheduler, AFK_LIST, TASK_LIST, TIME_LIST, this)
        }
        return if (command.name.equals("re", ignoreCase = true)) {
            CommandHelper.handleReCommand(
                sender,
                args,
                scheduler,
                AFK_LIST,
                TASK_LIST,
                TIME_LIST,
                this
            )
        } else false
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val joiningPlayer = event.player
        CommandHelper.setPlayerAfk(joiningPlayer, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this)
        scheduler.scheduleSyncDelayedTask(this, {
            if (AFK_LIST.isNotEmpty()) {
                for (i in 0..<AFK_LIST.size) {
                    val player = server.getPlayer(AFK_LIST[i])

                    player?.playerListName(getPlayerListName(player, true))
                }
            }
        }, TIME_TILL_PLAYER_IS_CHECKED_AFK)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val leavingPlayer = event.player
        CommandHelper.setPlayerBackAfterLeaveOrKick(leavingPlayer, scheduler, AFK_LIST, TASK_LIST, TIME_LIST)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        val kickedPlayer = event.player
        CommandHelper.setPlayerBackAfterLeaveOrKick(kickedPlayer, scheduler, AFK_LIST, TASK_LIST, TIME_LIST)
    }

    @EventHandler
    fun onPlayerMoveEvent(event: PlayerMoveEvent) {
        val movingPlayer = event.player
        val from = event.from
        val to = event.to

        handlePlayerAction(movingPlayer, to.distance(from) > MOVEMENT_DISTANCE)
    }

    @EventHandler
    fun onAsyncPlayerChatEvent(event: AsyncChatEvent) {
        val chattingPlayer = event.player

        handlePlayerAction(chattingPlayer, false)
    }

    @EventHandler
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        val interactingPlayer = event.player

        handlePlayerAction(interactingPlayer, true)
    }

    @EventHandler
    fun onPlayerFishEvent(event: PlayerFishEvent) {
        val fishingPlayer = event.player

        handlePlayerAction(fishingPlayer, true)
    }

    private fun handlePlayerAction(player: Player, isNotAfk: Boolean) {
        TASK_LIST[player]?.let { scheduler.cancelTask(it) }

        if (!AFK_LIST.contains(player.uniqueId)) {
            CommandHelper.setPlayerAfk(player, scheduler, AFK_LIST, LAST_ACTIVE, TASK_LIST, TIME_LIST, this)
        }
        if (isNotAfk) {
            CommandHelper.setPlayerBack(player, scheduler, AFK_LIST, TASK_LIST, TIME_LIST, this, false)
        }
    }
}