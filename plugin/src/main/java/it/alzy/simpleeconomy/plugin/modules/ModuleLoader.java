package it.alzy.simpleeconomy.plugin.modules;

import it.alzy.simpleeconomy.api.EconomyModule;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.model.LoadedModule;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleLoader {


    public static LoadedModule loadModule(File file, SimpleEconomy plugin) throws Exception {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) {
                throw new Exception("module.yml not found in jar");
            }
            YamlConfiguration moduleConfig;
            try (InputStream is = jar.getInputStream(entry)) {
                moduleConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
            }
            String mainClass = moduleConfig.getString("main");
            String moduleName = moduleConfig.getString("name");

            if (mainClass == null || moduleName == null) {
                throw new Exception("main or name not found in module.yml");
            }

            URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, plugin.getClass().getClassLoader());
            Class<?> clazz = loader.loadClass(mainClass);
            try {
                clazz = Class.forName(mainClass, true, loader);
            } catch (ClassNotFoundException e) {
                loader.close();
                throw new Exception("Main class not found for module " + moduleName, e);
            }

            if(!EconomyModule.class.isAssignableFrom(clazz)) {
                throw new Exception("Main class is not an instance of EconomyModule for " + moduleName);
            }

            EconomyModule instance;
            try {
                instance = (EconomyModule) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                loader.close();
                throw new Exception("Failed to create instance of EconomyModule for " + moduleName, e);
            }

            File moduleFolder = new File(plugin.getDataFolder(), "modules/" + moduleName);
            if(!moduleFolder.exists() && !moduleFolder.mkdirs()) {
                loader.close();
                throw new Exception("Failed to create module folder for " + moduleName);
            }

            return new LoadedModule(instance, loader, file, moduleFolder);
        }
    }
}
