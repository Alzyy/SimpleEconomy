package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadPoolExecutor;

@CommandAlias("simpleconomy|se|simpleeconomy")
@Description("Main command for SimpleEconomy")
public class SECommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Default
    public void root(Player player) {
        @SuppressWarnings("deprecation")
        String currentVersion = plugin.getDescription().getVersion();
        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gradient:#22C55E:#16A34A><bold>✔ SimpleEconomy</bold></gradient> <gray>| Version </gray><white>"
                        + currentVersion + "</white>\n"
                        + "<gray>Developed with ❤ by </gray>"
                        + "<hover:show_text:'Click to view my profile!'><click:open_url:'https://www.spigotmc.org/members/alzyit.1581572/'>"
                        + "<gradient:#A1A1AA:#71717A><bold>AlzyIT</bold></gradient></click></hover>"));
    }

    @Subcommand("reload")
    @Description("Reloads the plugin configurations")
    public void reload(CommandSender commandSender) {
        if (!commandSender.hasPermission("simpleconomy.command.reload")) {
            languageManager.send(commandSender, LanguageKeys.NO_PERMISSION, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        plugin.getExecutor().execute(() -> {
            SettingsConfig.getInstance().reload();
            languageManager.reloadAll(commandSender);
        });
    }

    @Subcommand("diagnose")
    @Description("Diagnose the plugin performance")
    public void diagnose(CommandSender sender) {
        if (!sender.hasPermission("simpleconomy.command.diagnose")) {
            languageManager.send(sender, LanguageKeys.NO_PERMISSION,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        plugin.runAsync(() -> {
            int cacheSize = plugin.getCache().getAll().size();
            int dirtyEntries = plugin.getCache().getDirtySize();
            String dbType = SettingsConfig.getInstance().storageSystem();

            int activeTasks = 0;
            int queueSize = 0;

            if (plugin.getExecutor() instanceof ThreadPoolExecutor pool) {
                activeTasks = pool.getActiveCount();
                queueSize = pool.getQueue().size();
            }

            languageManager.send(sender, LanguageKeys.DIAGNOSE_RESULT,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%cacheSize%", String.valueOf(cacheSize),
                    "%dirtyEntries%", String.valueOf(dirtyEntries),
                    "%dbType%", dbType,
                    "%activeTasks%", String.valueOf(activeTasks),
                    "%threadPoolSize%", String.valueOf(SettingsConfig.getInstance().getThreadPoolSize()),
                    "%queueSize%", String.valueOf(queueSize)
            );
        });
    }


}
