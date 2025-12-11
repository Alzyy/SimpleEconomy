package it.alzy.simpleeconomy.plugin.i18n;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.i18n.enums.LanguageKeys;
import it.alzy.simpleeconomy.plugin.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {

    private final ConcurrentHashMap<String, YamlConfiguration> languages = new ConcurrentHashMap<>();

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
                    String code = file.getName().replace("lang_", "").replace(".yml", "");
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    languages.put(code, cfg);
                }
            }
        });
    }

    public void reload(String language) {
     setActiveLanguage(language);
        try {
            File file = new File(plugin.getDataFolder(), "languages/lang_" + activeLanguage + ".yml");
            if (!file.exists()) {
                plugin.getLogger().warning("Language file for '" + activeLanguage + "' not found. Reverting it to english.");
                activeLanguage = "en";
                return;
            }

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            languages.put(activeLanguage, cfg);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload language '" + activeLanguage + "': " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void unloadLanguages() {
        languages.clear();
    }

    public void setActiveLanguage(String language) {
        if (languages.containsKey(language)) {
            activeLanguage = language;
            plugin.getLogger().info("Active language set to: " + language);
        } else {
            activeLanguage = "en";
            plugin.getLogger().warning("Language '" + language + "' not found. Defaulting to English.");
        }
    }

    public String getString(String path) {
        YamlConfiguration config = languages.getOrDefault(activeLanguage, languages.get("en"));
        return config.getString(path, "Message " + path + " not found.");
    }

    public void send(CommandSender player, LanguageKeys msg, Object... placeholders) {
        plugin.getExecutor().execute(() -> {
            YamlConfiguration config = languages.getOrDefault(activeLanguage, languages.get("en"));
            String message = config.getString(msg.path(), "Message " + msg.path() + " not found.");
            Bukkit.getScheduler().runTask(plugin, () -> ChatUtils.send(player, message, placeholders));
        });
    }

    public String getMessage(LanguageKeys msg) {
        return getString(msg.path());
    }
}
