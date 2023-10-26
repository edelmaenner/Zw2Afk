package de.zweistein2.plugins.zw2afk.util

import de.zweistein2.plugins.zw2afk.Zw2Afk
import github.scarsz.discordsrv.DiscordSRV
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Statistic
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitScheduler
import ru.tehkode.permissions.bukkit.PermissionsEx
import java.lang.String.join
import java.util.*

object CommandHelper {
    private const val TIME_TILL_PLAYER_IS_IGNORED_BY_SLEEP_CHECK = 1200L
    private const val TIME_TILL_PLAYER_IS_DECLARED_AFK = 18000L
    private const val COLOR_CODECS = "&((?i)[0-9a-fk-or])"
    private val LEGACY_COLOR_DESERIALIZER = LegacyComponentSerializer.legacyAmpersand()

    fun handleAfkCommand(
        sender: CommandSender,
        args: Array<out String>?,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        taskList: MutableMap<Player, Int>,
        timeList: MutableMap<Player, Int>,
        instance: Zw2Afk
    ): Boolean {
        if (sender !is Player) {
            instance.logger.info("Dieser Befehl ist nur für Spieler!")
            return true
        }
        return setPlayerAfkAfterCommand(
            args, sender, scheduler, afkList, taskList, timeList, instance
        )
    }

    fun handleReCommand(
        sender: CommandSender,
        args: Array<out String>?,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        taskList: Map<Player, Int>,
        timeList: Map<Player, Int>,
        instance: Zw2Afk
    ): Boolean {
        if (args != null && args.isEmpty()) {
            if (sender !is Player) {
                instance.logger.info("Dieser Befehl ist nur für Spieler!")
                return true
            }
            return setPlayerBack(sender, scheduler, afkList, taskList, timeList, instance, true)
        }
        return false
    }

    fun setPlayerBack(
        sendingPlayer: Player,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        taskList: Map<Player, Int>,
        timeList: Map<Player, Int>,
        instance: Zw2Afk,
        feedback: Boolean
    ): Boolean {
        if (taskList.isNotEmpty()) {
            taskList[sendingPlayer]?.let { scheduler.cancelTask(it) }
        }
        if (afkList.contains(sendingPlayer.uniqueId)) {
            afkList.remove(sendingPlayer.uniqueId)
            val message = sendingPlayer.name + " ist wieder da"
            instance.server.broadcast(Component.text(message, NamedTextColor.GOLD))
            if (Zw2Afk.SUPPORTED_PLUGINS["DiscordSRV"] == true) {
                if (DiscordSRV.isReady) {
                    DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global").sendMessage(message)
                        .queue()
                }
            }
            sendingPlayer.playerListName(getPlayerListName(sendingPlayer, false))
            timeList[sendingPlayer]?.let { sendingPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, it) }
            sendingPlayer.isSleepingIgnored = false
        } else if (feedback) {
            sendingPlayer.sendMessage(Component.text("Du bist gar nicht afk!", NamedTextColor.RED))
            return false
        }
        return true
    }

    private fun setPlayerAfkAfterCommand(
        args: Array<out String>?,
        sendingPlayer: Player,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        taskList: MutableMap<Player, Int>,
        timeList: MutableMap<Player, Int>,
        instance: Zw2Afk
    ): Boolean {
        if (!afkList.contains(sendingPlayer.uniqueId)) {
            if (taskList.isNotEmpty()) {
                taskList[sendingPlayer]?.let { scheduler.cancelTask(it) }
            }

            val message =
                sendingPlayer.name + " ist afk" + if (args == null || args.isEmpty()) "" else ": " + join(" ", *args)

            instance.server.broadcast(Component.text(message, NamedTextColor.GOLD))
            if (Zw2Afk.SUPPORTED_PLUGINS["DiscordSRV"] == true) {
                if (DiscordSRV.isReady) {
                    DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global").sendMessage(message)
                        .queue()
                }
            }
            afkList.add(sendingPlayer.uniqueId)
            sendingPlayer.playerListName(getPlayerListName(sendingPlayer, true))
            timeList[sendingPlayer] = sendingPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE)
            taskList[sendingPlayer] = scheduler.scheduleSyncDelayedTask(
                instance, { sendingPlayer.isSleepingIgnored = true }, TIME_TILL_PLAYER_IS_IGNORED_BY_SLEEP_CHECK
            )
        } else {
            sendingPlayer.sendMessage(Component.text("Du bist schon afk!", NamedTextColor.RED))
            return false
        }
        return true
    }

    fun setPlayerAfk(
        player: Player,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        lastActive: MutableMap<Player, Long>,
        taskList: MutableMap<Player, Int>,
        timeList: MutableMap<Player, Int>,
        instance: Zw2Afk
    ) {
        lastActive[player] = System.currentTimeMillis()
        taskList[player] = scheduler.scheduleSyncDelayedTask(instance, {
            if (lastActive[player] != System.currentTimeMillis()) {
                val message = player.name + " ist afk"
                instance.server.broadcast(Component.text(message, NamedTextColor.GOLD))
                if (Zw2Afk.SUPPORTED_PLUGINS["DiscordSRV"] == true) {
                    if (DiscordSRV.isReady) {
                        DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global")
                            .sendMessage(message).queue()
                    }
                }
                afkList.add(player.uniqueId)
                player.playerListName(getPlayerListName(player, true))
                timeList[player] = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
                player.isSleepingIgnored = true
            }
        }, TIME_TILL_PLAYER_IS_DECLARED_AFK)
    }

    fun setPlayerBackAfterLeaveOrKick(
        kickedPlayer: Player,
        scheduler: BukkitScheduler,
        afkList: MutableList<UUID>,
        taskList: Map<Player, Int>,
        timeList: Map<Player, Int>
    ) {
        if (taskList.isNotEmpty()) {
            taskList[kickedPlayer]?.let { scheduler.cancelTask(it) }
        }
        if (afkList.contains(kickedPlayer.uniqueId)) {
            afkList.remove(kickedPlayer.uniqueId)
            timeList[kickedPlayer]?.let { kickedPlayer.setStatistic(Statistic.PLAY_ONE_MINUTE, it) }
            kickedPlayer.isSleepingIgnored = false
        }
    }

    fun getPlayerListName(player: Player, isAfk: Boolean): Component {
        return if(isAfk) {
            if (Zw2Afk.SUPPORTED_PLUGINS["PermissionsEx"] == true) {
                val user = PermissionsEx.getPermissionManager().getUser(player)
                if (user != null) Component.textOfChildren(
                    Component.text(user.prefix.replace(COLOR_CODECS.toRegex(), ""), NamedTextColor.GRAY),
                    Component.text(user.name.replace(COLOR_CODECS.toRegex(), ""), NamedTextColor.GRAY),
                    Component.text(user.suffix.replace(COLOR_CODECS.toRegex(), ""), NamedTextColor.GRAY)
                ) else Component.text(player.name, NamedTextColor.GRAY)
            } else {
                Component.text(player.name, NamedTextColor.GRAY)
            }
        } else {
            if (Zw2Afk.SUPPORTED_PLUGINS["PermissionsEx"] == true) {
                val user = PermissionsEx.getPermissionManager().getUser(player)
                if (user != null) Component.textOfChildren(
                    LEGACY_COLOR_DESERIALIZER.deserialize(user.prefix),
                    LEGACY_COLOR_DESERIALIZER.deserialize(user.name),
                    LEGACY_COLOR_DESERIALIZER.deserialize(user.suffix)
                ) else Component.text(player.name)
            } else {
                Component.text(player.name)
            }
        }
    }
}