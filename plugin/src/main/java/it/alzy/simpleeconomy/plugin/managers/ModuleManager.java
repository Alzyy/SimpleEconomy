package it.alzy.simpleeconomy.plugin.managers;

import it.alzy.simpleeconomy.api.EconomyModule;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.modules.ModuleLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleManager {

    private final SimpleEconomy plugin;
    private final List<EconomyModule> modules = new ArrayList<>();
    private final File modulesFolder;

    public ModuleManager(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.modulesFolder = new File(plugin.getDataFolder(), "modules");
        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs();
        }
    }

    public void loadModules() {
        plugin.getLogger().info("Loading external modules...");
        File[] files = modulesFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (files == null || files.length == 0) {
            plugin.getLogger().info("No modules found to load.");
            return;
        }

        for (File f : files) {
            try {
                EconomyModule module = ModuleLoader.loadModule(f, plugin);
                modules.add(module);
                plugin.getLogger().info("Successfully loaded module: " + f.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load module from " + f.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Finished loading modules. Total: " + modules.size());
    }

    public void unloadModule(EconomyModule module) {
        try {
            module.onDisable();
        } catch (Exception e) {
            plugin.getLogger().severe("Error while disabling module: " + e.getMessage());
        }
        modules.clear();
    }

    public Set<String> getLoadedModuleNames() {
        return modules.stream().map(EconomyModule::getName).collect(Collectors.toSet());
    }

    public List<EconomyModule> getModules() {
        return new ArrayList<>(modules);
    }

    public void enableModule(String moduleName) {
        for (EconomyModule module : modules) {
            if (module.getName().equalsIgnoreCase(moduleName)) {
                try {
                    module.onEnable(plugin, new File(modulesFolder, moduleName));
                    plugin.getLogger().info("Module " + moduleName + " has been enabled.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to enable module " + moduleName + ": " + e.getMessage());
                }
                return;
            }
        }
        plugin.getLogger().warning("Module " + moduleName + " not found.");
    }

    public void disableModule(String moduleName) {
        for (EconomyModule module : modules) {
            if (module.getName().equalsIgnoreCase(moduleName)) {
                try {
                    module.onDisable();
                    plugin.getLogger().info("Module " + moduleName + " has been disabled.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to disable module " + moduleName + ": " + e.getMessage());
                }
                return;
            }
        }
        plugin.getLogger().warning("Module " + moduleName + " not found.");
    }

    public void loadModuleFromFile(String fileName) {
        File file = new File(modulesFolder, fileName);
        if (!file.exists() || !file.getName().endsWith(".jar")) {
            plugin.getLogger().warning("Module file " + fileName + " not found or is not a .jar file.");
            return;
        }
        try {
            EconomyModule module = ModuleLoader.loadModule(file, plugin);
            modules.add(module);
            plugin.getLogger().info("Successfully loaded module from file: " + fileName);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load module from file " + fileName + ": " + e.getMessage());
        }
    }

    public boolean doesFileExists(String fileName) {
        File file = new File(modulesFolder, fileName);
        return file.exists() && file.getName().endsWith(".jar");
    }

}