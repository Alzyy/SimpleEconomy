package it.alzy.simpleeconomy.plugin.modules;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.configuration.file.YamlConfiguration;

import it.alzy.simpleeconomy.api.EconomyModule;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;

public class ModuleLoader {

    public static EconomyModule loadModule(File file, SimpleEconomy plugin) throws Exception {
        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if(entry == null) {
                throw new Exception("module.yml not found in " + file.getName());
            }
            InputStream is = jar.getInputStream(entry);
            YamlConfiguration moduleConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(is));

            String mainClass = moduleConfiguration.getString("main");
            String name = moduleConfiguration.getString("name");
            if(mainClass == null || name == null) {
                throw new Exception("main or name not defined in module.yml of " + file.getName());
            }

            URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, plugin.getClass().getClassLoader());
            Class<?> clazz = Class.forName(mainClass, true, loader);
            EconomyModule module = (EconomyModule) clazz.getDeclaredConstructor().newInstance();
            File moduleFolder = new File(plugin.getDataFolder(), "modules/" + name);
            if(!moduleFolder.exists()) {
                moduleFolder.mkdirs();
            }
            module.onEnable(plugin, moduleFolder);
            return module;
        }
    }

}
