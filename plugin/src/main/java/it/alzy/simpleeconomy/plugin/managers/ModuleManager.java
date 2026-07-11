package it.alzy.simpleeconomy.plugin.managers;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import it.alzy.simpleeconomy.api.SimpleEconomyAPI;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.model.LoadedModule;
import it.alzy.simpleeconomy.plugin.modules.ModuleLoader;

public class ModuleManager {

    private final SimpleEconomy plugin;
    private final Map<String, LoadedModule> modules = new LinkedHashMap<>();
    private final File modulesFolder;

    public ModuleManager(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.modulesFolder = new File(plugin.getDataFolder(), "modules");
        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs();
        }
    }

    private static String key(String name) {
        return name.toLowerCase();
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
                LoadedModule loaded = ModuleLoader.loadModule(f, plugin);
                String moduleKey = key(loaded.getInstance().getName());

                if (modules.containsKey(moduleKey)) {
                    plugin.getLogger().warning("Duplicate module ignored: " + loaded.getInstance().getName()
                            + " (file " + f.getName() + ")");
                    loaded.closeClassLoader();
                    continue;
                }

                modules.put(moduleKey, loaded);
                enable(loaded);
                plugin.getLogger().info("Module loaded and enabled: " + loaded.getInstance().getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't load module from " + f.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + modules.size() + " module(s)");
    }

    private void enable(LoadedModule loaded) {
        if (loaded.isEnabled()) return;
        if (!SimpleEconomyAPI.isProviderSet()) return;
        try {
            loaded.getInstance().onEnable(plugin, loaded.getDataFolder());
            loaded.setEnabled(true);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't enable module " + loaded.getInstance().getName(), e);
        }
    }

    private void disable(LoadedModule loaded) {
        if (!loaded.isEnabled()) return;
        if (!SimpleEconomyAPI.isProviderSet()) return;
        try {
            loaded.getInstance().onDisable();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't disable module " + loaded.getInstance().getName(), e);
        } finally {
            loaded.setEnabled(false);
        }
    }

    public void enableModule(String moduleName) {
        LoadedModule loaded = modules.get(key(moduleName));
        if (loaded == null) {
            plugin.getLogger().warning("Module " + moduleName + " not found.");
            return;
        }
        if (!SimpleEconomyAPI.isProviderSet()) {
            plugin.getLogger().warning("Cannot enable " + moduleName + ": EconomyProvider not set.");
            return;
        }
        boolean wasEnabled = loaded.isEnabled();
        enable(loaded);
        if (loaded.isEnabled() && !wasEnabled) {
            plugin.getLogger().info("Enabled module " + moduleName + ".");
        }
    }

    public void disableModule(String moduleName) {
        LoadedModule loaded = modules.get(key(moduleName));
        if (loaded == null) {
            plugin.getLogger().warning("Module " + moduleName + " not found.");
            return;
        }
        if (!SimpleEconomyAPI.isProviderSet()) {
            plugin.getLogger().warning("Cannot disable " + moduleName + ": EconomyProvider not set.");
            return;
        }
        boolean wasEnabled = loaded.isEnabled();
        disable(loaded);
        if (!loaded.isEnabled() && wasEnabled) {
            plugin.getLogger().info("Disabled module " + moduleName + ".");
        }
    }

    public void unloadModule(String moduleName) {
        LoadedModule loaded = modules.get(key(moduleName));
        if (loaded == null) {
            plugin.getLogger().warning("Module " + moduleName + " not found.");
            return;
        }
        disable(loaded);
        try {
            loaded.closeClassLoader();
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing classloader of " + moduleName, e);
        }
        modules.remove(key(moduleName));
        plugin.getLogger().info("Module " + moduleName + " unloaded.");
    }

    public void loadModuleFromFile(String fileName) {
        File file = new File(modulesFolder, fileName);
        if (!file.exists() || !file.getName().endsWith(".jar")) {
            plugin.getLogger().warning("File modulo " + fileName + " not found or invalid.");
            return;
        }

        try {
            LoadedModule loaded = ModuleLoader.loadModule(file, plugin);
            String moduleKey = key(loaded.getInstance().getName());

            if (modules.containsKey(moduleKey)) {
                unloadModule(loaded.getInstance().getName());
                plugin.getLogger().info("Module " + loaded.getInstance().getName() + " was already loaded, unloaded to be reloaded.");
            }

            modules.put(moduleKey, loaded);
            enable(loaded);
            plugin.getLogger().info("Module loaded from file: " + fileName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't load module from file " + fileName, e);
        }
    }

    public boolean doesFileExists(String fileName) {
        File file = new File(modulesFolder, fileName);
        return file.exists() && file.getName().endsWith(".jar");
    }

    public Set<String> getLoadedModuleNames() {
        return modules.values().stream()
                .map(m -> m.getInstance().getName())
                .collect(Collectors.toSet());
    }

    public Collection<LoadedModule> getModules() {
        return modules.values();
    }

    public LoadedModule getModule(String moduleName) {
        return modules.get(key(moduleName));
    }
}
