package de.zweistein2.plugins.zw2afk.util;

import org.bukkit.entity.Player;
import ru.tehkode.permissions.PermissionUser;

public class PermissionsExHelper
{
    private PermissionsExHelper() {}

    public static String getPlayerListName(final Player player) {
        final PermissionUser user = ru.tehkode.permissions.bukkit.PermissionsEx.getPermissionManager().getUser(player);
        if (user == null) {
            return player.getName();
        } else {
            return user.getPrefix() + user.getName() + user.getSuffix();
        }
    }
}
