package it.alzy.simpleeconomy.plugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class UpdateUtils {

    private static final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private static final String API_URL = "https://api.spiget.org/v2/resources/127423/versions/latest";
    private static final Gson gson = new Gson();

    @Getter private static volatile boolean isUpdateAvailable = false;
    @Getter private static volatile String newVersion = "";

    public void checkForUpdates() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return fetchJson();
            } catch (Exception e) {
                return null;
            }
        }, plugin.getExecutor()).thenAccept(json -> {
            if (json == null) return;

            try {
                JsonObject obj = gson.fromJson(json, JsonObject.class);

                if (!obj.has("name")) return;

                String latestVersion = obj.get("name").getAsString();
                String currentVersion = plugin.getDescription().getVersion();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    isUpdateAvailable = true;
                    newVersion = latestVersion;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().info(" ");
                        plugin.getLogger().info("  A NEW UPDATE IS AVAILABLE!");
                        plugin.getLogger().info("  Current version: " + currentVersion);
                        plugin.getLogger().info("  Latest version: " + latestVersion);
                        plugin.getLogger().info("  Download: https://www.spigotmc.org/resources/127423/");
                        plugin.getLogger().info(" ");
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getLogger().info("You are running the latest version (" + currentVersion + ")."));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse update data from Spiget.");
            }
        });
    }

    private String fetchJson() throws Exception {
        URL url = new URI(API_URL).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SimpleEconomy-Updater");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int responseCode = con.getResponseCode();
        if (responseCode != 200) return null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            con.disconnect();
        }
    }
}