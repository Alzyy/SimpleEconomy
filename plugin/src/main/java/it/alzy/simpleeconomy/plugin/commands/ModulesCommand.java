package it.alzy.simpleeconomy.plugin.commands;

import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.LanguageManager;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;

@CommandAlias("modules")
@CommandPermission("simpleeconomy.command.modules")
public class ModulesCommand extends BaseCommand {
    

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private final LanguageManager languageManager = plugin.getLanguageManager();

    @Subcommand("list")
    public void onList(Player player) {
        if(plugin.getModuleManager().getModules().isEmpty()) {
            plugin.getLanguageManager().send(player, LanguageKeys.MODULES_NO_MODULES,"%prefix%", languageManager.getMessage(LanguageKeys.PREFIX));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (var module : plugin.getModuleManager().getModules()) {
            sb.append(module.getName()).append(", ");
        }
        plugin.getLanguageManager().send(player, LanguageKeys.MODULES_LIST, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%modules%", sb.substring(0, sb.length() - 2));
    }

    @Subcommand("disable")
    @CommandCompletion("@modules")
    public void onDisable(Player player, String moduleName) {
        plugin.getModuleManager().unloadModule(plugin.getModuleManager().getModules().stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null));
        plugin.getLanguageManager().send(player, LanguageKeys.MODULE_DISABLED, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%module%", moduleName);
    }

    @Subcommand("status")
    @CommandCompletion("@modules")
    public void onStatus(Player player, String moduleName) {
        if(plugin.getModuleManager().getLoadedModuleNames().stream().noneMatch(m -> m.equalsIgnoreCase(moduleName))) {
            plugin.getLanguageManager().send(player, LanguageKeys.MODULE_NOT_FOUND, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%module%", moduleName);
            return;
        }
        boolean isEnabled = plugin.getModuleManager().getModules().stream()
                .anyMatch(m -> m.getName().equalsIgnoreCase(moduleName));
        plugin.getLanguageManager().send(player, LanguageKeys.MODULES_STATUS, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%module%", moduleName, "%status%", isEnabled ? languageManager.getMessage(LanguageKeys.MODULE_ENABLED_PLACEHOLDER) : languageManager.getMessage(LanguageKeys.MODULE_DISABLED_PLACEHOLDER));
    }

    @Subcommand("loadfromfile")
    @CommandCompletion("@modulesFiles")
    public void onLoadFromFile(Player player, String fileName) {
        if(!plugin.getModuleManager().doesFileExists(fileName)) {
            plugin.getLanguageManager().send(player, LanguageKeys.MODULE_COULDNT_FIND_FILE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%file%", fileName);
            return;
        }
        plugin.getModuleManager().loadModuleFromFile(fileName);
        plugin.getLanguageManager().send(player, LanguageKeys.MODULE_LOADED_FROM_FILE, "%prefix%", languageManager.getMessage(LanguageKeys.PREFIX), "%file%", fileName);
    }
}
