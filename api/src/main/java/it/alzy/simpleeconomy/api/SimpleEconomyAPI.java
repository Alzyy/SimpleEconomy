package it.alzy.simpleeconomy.api;

/**
 * Central access point for the SimpleEconomy API.
 *
 * <p>This class provides static methods to get or set the {@link EconomyProvider} implementation
 * that handles all economy operations. Plugins should call {@link #setProvider(EconomyProvider)}
 * during initialization to register their economy provider, and then other plugins or modules
 * can retrieve it via {@link #getProvider()}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Setting the provider (usually done in your plugin's onEnable)
 * SimpleEconomyAPI.setProvider(new MyEconomyProvider(plugin));
 *
 * // Retrieving the provider elsewhere
 * EconomyProvider provider = SimpleEconomyAPI.getProvider();
 * provider.getBalance(player.getUniqueId()).thenAccept(balance -> {
 *     System.out.println("Player balance: " + balance);
 * });
 * }</pre>
 *
 * <p><b>Note:</b> Only one provider can be set. Attempting to set it again
 * will throw an {@link IllegalStateException}.</p>
 */
public class SimpleEconomyAPI {

    /** The singleton instance of the registered economy provider. */
    private static EconomyProvider provider;

    /** Private constructor to prevent instantiation. */
    private SimpleEconomyAPI() {}

    /**
     * Returns the currently registered {@link EconomyProvider}.
     *
     * @return the registered economy provider
     * @throws IllegalStateException if no provider has been set yet
     */
    public static EconomyProvider getProvider() {
        if (provider == null) {
            throw new IllegalStateException(
                    "EconomyProvider is not initialized. Make sure the plugin is enabled."
            );
        }
        return provider;
    }

    /**
     * Registers the {@link EconomyProvider} to be used by the API.
     * <p>
     * This method can only be called once. Attempting to set a provider
     * when one is already registered will result in an exception.
     * </p>
     *
     * @param economyProvider the provider implementation to register
     * @throws IllegalStateException if a provider has already been set
     */
    public static void setProvider(EconomyProvider economyProvider) {
        if (provider != null) {
            throw new IllegalStateException("EconomyProvider is already set.");
        }
        provider = economyProvider;
    }
}
