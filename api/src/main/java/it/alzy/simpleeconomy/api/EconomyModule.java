package it.alzy.simpleeconomy.api;

import java.io.File;

/**
 * Lifecycle contract for SimpleEconomy modules.
 */
public interface EconomyModule {

    /**
     * Initializes the module with the core plugin instance and a data folder.
     *
     * @param core the core plugin instance (implementation-specific type)
     * @param moduleFolder the module's data folder
     */
    void onEnable(Object core, File moduleFolder);

    /**
     * Shuts down the module and releases resources.
     */
    void onDisable();

    /**
     * Returns the name of the module.
     *
     * @return the module name
     */
    String getName();
}
