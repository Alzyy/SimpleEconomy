package it.alzy.simpleeconomy.plugin.model;

import java.io.File;
import java.net.URLClassLoader;

import it.alzy.simpleeconomy.api.EconomyModule;
import lombok.Data;

@Data
public class LoadedModule {

    private final EconomyModule instance;
    private final URLClassLoader classLoader;
    private final File file;
    private final File dataFolder;
    private boolean enabled = false;


    public LoadedModule(EconomyModule instance, URLClassLoader classLoader, File file, File dataFolder) {
        this.instance = instance;
        this.classLoader = classLoader;
        this.file = file;
        this.dataFolder = dataFolder;
    }

    public void closeClassLoader() {
        try {
            classLoader.close();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't close class loader of " + instance.getName(), e);
        }
    }

}
