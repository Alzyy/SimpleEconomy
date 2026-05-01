package it.alzy.simpleeconomy.plugin.i18n;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
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
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {

    private final ConcurrentHashMap<String, YamlConfiguration> languages = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Map<String, String>> cachedMessages = new ConcurrentHashMap<>();
    @Setter
    private volatile String activeLanguage;
    private final SimpleEconomy plugin;

    public LanguageManager(SimpleEconomy plugin, String lang) {
        this.plugin = plugin;
        this.activeLanguage = lang;
        loadLanguages();
    }

    public void loadLanguages() {
        plugin.getExecutor().execute(() -> {
            plugin.getLogger().info("Loading language files...");
            File langFolder = new File(plugin.getDataFolder(), "languages");
            if (!langFolder.exists()) {
                langFolder.mkdirs();
                plugin.saveResource("languages/lang_it.yml", false);
                plugin.saveResource("languages/lang_en.yml", false);
                plugin.getLogger().info("Default language files created.");
            }

            for (File file : Objects.requireNonNull(langFolder.listFiles())) {
                if (file.getName().endsWith(".yml")) {
                    updateAndLoad(file);
                }
            }
        });
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
            languages.put(code, config);

            Map<String, String> cache = new ConcurrentHashMap<>();
            for(String key : config.getKeys(true)) {
                if(config.isString(key)) {
                    cache.put(key, config.getString(key));
                }
            }
            cachedMessages.put(code, cache);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading language file " + file.getName() + ": " + e.getMessage());
        }
    }

    public void reload(String language) {
        setActiveLanguage(language);
        plugin.getExecutor().execute(() -> {
            try {
                File file = new File(plugin.getDataFolder(), "languages/lang_" + activeLanguage + ".yml");
                if (!file.exists()) {
                    plugin.getLogger().warning("Language file for '" + activeLanguage + "' not found. Reverting to english.");
                    activeLanguage = "en";
                    return;
                }
                updateAndLoad(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to reload language '" + activeLanguage + "': " + e.getMessage());
            }
        });
    }

    public void unloadLanguages() {
        languages.clear();
        cachedMessages.clear();
    }

    public String getString(String path) {
        YamlConfiguration config = languages.getOrDefault(activeLanguage, languages.get("en"));
        if (config == null) return "Language Error: No config loaded.";
        return config.getString(path, "Message " + path + " not found.");
    }


    public void send(CommandSender sender, LanguageKeys msg, Object... placeholders) {
        String langCode = (sender instanceof Player p) ? p.locale().getLanguage() : activeLanguage;

        Map<String, String> langMap = cachedMessages.getOrDefault(langCode, cachedMessages.get("en"));
        String message = (langMap != null) ? langMap.get(msg.path()) : "§cKey not found";

        ChatUtils.send(sender, message, placeholders);
    }

    public String getMessage(LanguageKeys msg) {
        return getString(msg.path());
    }
}