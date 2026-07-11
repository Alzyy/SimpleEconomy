package it.alzy.simpleeconomy.plugin.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.model.LoadedModule;

import org.bukkit.entity.Player;

@CommandAlias("modules")
@CommandPermission("simpleeconomy.command.modules")
@Description("Manage the modules in your modules folder.")
public class ModulesCommand extends BaseCommand {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Subcommand("list")
    public void onList(Player player) {
        if (plugin.getModuleManager().getModules().isEmpty()) {
            languageManager.send(player, LanguageKeys.MODULES_NO_MODULES,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (LoadedModule module : plugin.getModuleManager().getModules()) {
            sb.append(module.getInstance().getName()).append(", ");
        }
        sb.setLength(sb.length() - 2);

        languageManager.send(player, LanguageKeys.MODULES_LIST,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%modules%", sb.toString());
    }

    @Subcommand("disable")
    @CommandCompletion("@modules")
    public void onDisable(Player player, String moduleName) {
        LoadedModule loaded = plugin.getModuleManager().getModule(moduleName);
        if (loaded == null) {
            languageManager.send(player, LanguageKeys.MODULE_NOT_FOUND,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%module%", moduleName);
            return;
        }

        plugin.getModuleManager().disableModule(moduleName);
        languageManager.send(player, LanguageKeys.MODULE_DISABLED,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%module%", moduleName);
    }

    @Subcommand("status")
    @CommandCompletion("@modules")
    public void onStatus(Player player, String moduleName) {
        LoadedModule loaded = plugin.getModuleManager().getModule(moduleName);
        if (loaded == null) {
            languageManager.send(player, LanguageKeys.MODULE_NOT_FOUND,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%module%", moduleName);
            return;
        }

        boolean isEnabled = loaded.isEnabled();
        languageManager.send(player, LanguageKeys.MODULES_STATUS,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%module%", moduleName,
                "%status%", isEnabled
                        ? languageManager.getMessage(LanguageKeys.MODULE_ENABLED_PLACEHOLDER)
                        : languageManager.getMessage(LanguageKeys.MODULE_DISABLED_PLACEHOLDER));
    }

    @Subcommand("loadfromfile")
    @CommandCompletion("@modulesFiles")
    public void onLoadFromFile(Player player, String fileName) {
        if (!plugin.getModuleManager().doesFileExists(fileName)) {
            languageManager.send(player, LanguageKeys.MODULE_COULDNT_FIND_FILE,
                    "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                    "%file%", fileName);
            return;
        }

        plugin.getModuleManager().loadModuleFromFile(fileName);
        languageManager.send(player, LanguageKeys.MODULE_LOADED_FROM_FILE,
                "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX),
                "%file%", fileName);
    }
}
