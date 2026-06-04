package it.alzy.simpleeconomy.plugin.i18n;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.model.VirtualCurrency;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {

    private final ConcurrentHashMap<String, Map<String, String>> cachedMessages = new ConcurrentHashMap<>();
    private final SimpleEconomy plugin;
    @Setter
    private volatile String activeLanguage;

    public LanguageManager(SimpleEconomy plugin, String lang) {
        this.plugin = plugin;
        this.activeLanguage = lang;
        loadLanguages();
    }

    public CompletableFuture<Void> loadLanguages() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("Loading language files...");
            File langFolder = new File(plugin.getDataFolder(), "languages");
            if (!langFolder.exists()) {
                langFolder.mkdirs();
                plugin.saveResource("languages/lang_it.yml", false);
                plugin.saveResource("languages/lang_en.yml", false);
                plugin.getLogger().info("Default language files created.");
            }

            File[] files = langFolder.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.getName().endsWith(".yml")) {
                    updateAndLoad(file);
                }
            }
        }, plugin.getExecutor());
    }

    private void updateAndLoad(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String resourcePath = "languages/" + file.getName();
            InputStream resourceStream = plugin.getResource(resourcePath);

            if (resourceStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
                boolean changed = false;

                for (String key : defaultConfig.getKeys(true)) {
                    if (!config.contains(key)) {
                        config.set(key, defaultConfig.get(key));
                        changed = true;
                    }
                }

                if (changed) {
                    config.save(file);
                    plugin.getLogger().info("Updated language file: " + file.getName() + " with missing keys.");
                }
            }

            String code = file.getName().replace("lang_", "").replace(".yml", "");

            Map<String, String> langCache = new ConcurrentHashMap<>();
            for (String key : config.getKeys(true)) {
                if (config.isString(key)) {
                    langCache.put(key, Objects.requireNonNull(config.getString(key)));
                }
            }
            cachedMessages.put(code, langCache);

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading language file " + file.getName() + ": " + e.getMessage());
        }
    }

    public void reload(String language) {
        this.activeLanguage = language;
        loadLanguages().thenRun(() -> plugin.getLogger().info("Language reloaded: " + language));
    }

    public void unloadLanguages() {
        cachedMessages.clear();
    }

    public String getString(String path) {
        Map<String, String> langMap = cachedMessages.getOrDefault(activeLanguage, cachedMessages.get("en"));
        if (langMap == null) return "§cLanguage not loaded.";
        return langMap.getOrDefault(path, "§cMessage " + path + " not found.");
    }

    public void send(CommandSender sender, LanguageKeys msg, Object... placeholders) {
        String langCode = (sender instanceof Player p) ? p.locale().getLanguage() : activeLanguage;

        Map<String, String> langMap = cachedMessages.getOrDefault(langCode, cachedMessages.getOrDefault("en", new ConcurrentHashMap<>()));
        String message = langMap.getOrDefault(msg.path(), "§cKey not found: " + msg.path());

        ChatUtils.send(sender, message, placeholders);
    }

    public void sendCurrencyMessage(CommandSender sender, VirtualCurrency currency, LanguageKeys msg, Object... placeholders) {
        String langCode = (sender instanceof Player p) ? p.locale().getLanguage() : activeLanguage;
        Map<String, String> langMap = cachedMessages.getOrDefault(langCode, cachedMessages.getOrDefault("en", new ConcurrentHashMap<>()));

        String genericPath = msg.path();
        String currencySpecificPath = genericPath.replace("messages.", "messages.currencies." + currency.getName() + ".");

        String finalMessage = langMap.getOrDefault(currencySpecificPath, langMap.getOrDefault(genericPath, "§cKey not found"));

        ChatUtils.send(sender, finalMessage, placeholders);
    }

    public void setupCurrencyTranslations(VirtualCurrency currency) {
        CompletableFuture.runAsync(() -> {
            for (String langCode : cachedMessages.keySet()) {
                File file = new File(plugin.getDataFolder(), "languages/lang_" + langCode + ".yml");
                if (!file.exists()) continue;

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                boolean changed = false;

                LanguageKeys[] keysToCopy = {
                        LanguageKeys.BALANCE_CHECK_SELF,
                        LanguageKeys.BALANCE_CHECK_OTHER,
                        LanguageKeys.GAVE_MONEY,
                        LanguageKeys.RECEIVED_MONEY
                };

                for (LanguageKeys key : keysToCopy) {
                    String genericPath = key.path();
                    String currencyPath = genericPath.replace("messages.", "messages.currencies." + currency.getName() + ".");

                    if (!config.contains(currencyPath)) {
                        config.set(currencyPath, config.get(genericPath));
                        changed = true;
                    }
                }

                if (changed) {
                    try {
                        config.save(file);
                        updateAndLoad(file);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not auto-generate translations for " + currency.getName());
                    }
                }
            }
        }, plugin.getExecutor());
    }

    public void removeCurrencyMessages(String name) {
        CompletableFuture.runAsync(() -> {
            for (String langCode : cachedMessages.keySet()) {
                File file = new File(plugin.getDataFolder(), "languages/lang_" + langCode + ".yml");
                if (!file.exists()) continue;

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String sectionPath = "messages.currencies." + name;
                config.set(sectionPath, null);
                try {
                    config.save(file);
                    updateAndLoad(file);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not remove translations for " + name);
                }
            }
        }, plugin.getExecutor());
    }

    public String getMessage(LanguageKeys msg) {
        return getString(msg.path());
    }

    public void reloadAll(CommandSender sender) {
        loadLanguages().thenRun(() -> send(sender, LanguageKeys.RELOAD_SUCCESS, "%prefix%", getMessage(LanguageKeys.PREFIX)));
    }
}