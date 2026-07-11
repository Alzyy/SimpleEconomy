package it.alzy.simpleeconomy.api;

import java.io.File;
import java.util.logging.Logger;

/**
 * Contract exposed by the core plugin to external modules.
 * Modules depend only on this interface, never on the concrete plugin class.
 */
public interface EconomyCore {

    /**
     * @return the plugin's logger, for consistent log formatting/prefixing.
     */
    Logger getCoreLogger();

    /**
     * @return the plugin's own data folder (not the module's).
     */
    File getCoreDataFolder();

    /**
     * Looks up another loaded module by name, for cross-module integration.
     * Returns null if not found or not enabled.
     */
    EconomyModule getModule(String name);
    
}