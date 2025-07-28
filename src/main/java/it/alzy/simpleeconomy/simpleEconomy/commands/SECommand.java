package it.alzy.simpleeconomy.simpleEconomy.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import it.alzy.simpleeconomy.simpleEconomy.configurations.LangConfig;
import it.alzy.simpleeconomy.simpleEconomy.utils.ChatUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

@CommandAlias("simpleconomy|se")
@Description("Main command for SimpleEconomy")
public class SECommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LangConfig config = LangConfig.getInstance();

    @Default
    public void root(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gradient:#22C55E:#16A34A><bold>✔ SimpleEconomy</bold></gradient> <gray>| Version </gray><white>"
                        + plugin.getPluginMeta().getVersion() + "</white>\n"
                        + "<gray>Developed with ❤ by </gray>"
                        + "<hover:show_text:'Click to view my profile!'><click:open_url:'https://www.spigotmc.org/members/alzyit.1581572/'>"
                        + "<gradient:#A1A1AA:#71717A><bold>AlzyIT</bold></gradient></click></hover>"));
    }

    @Subcommand("reload")
    public void reload(Player player) {
        if (!player.hasPermission("simpleeconomy.command.reload")) {
            ChatUtils.send(player, config.NO_PERMISSION, "%prefix%", config.PREFIX);
            return;
        }

        plugin.reloadConfigurations();
        ChatUtils.send(player, config.RELOAD_SUCCESS, "%prefix%", config.PREFIX);
    }
}
