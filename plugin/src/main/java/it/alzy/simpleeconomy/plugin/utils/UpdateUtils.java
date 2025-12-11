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
    private static final String API_URL = "https://ministats.alzy.site/api/v1/updates/check/3";
    private static final Gson gson = new Gson();

    @Getter private static volatile boolean isUpdateAvailable = false;
    @Getter private static volatile String updateNotes = "";
    @Getter private static volatile String newVersion = "";

    public void checkForUpdates() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return fetchJson();
            } catch (Exception e) {
                return null;
            }
        }, plugin.getExecutor()).thenAccept(json -> plugin.getExecutor().execute(() -> {
                if (json == null) return;
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                String latest = obj.has("version") ? obj.get("version").getAsString() : "";
                if (latest.isEmpty()) return;

                String current = plugin.getDescription().getVersion();

                if (!current.equalsIgnoreCase(latest)) {
                    isUpdateAvailable = true;
                    newVersion = latest;
                    updateNotes = obj.has("messageUpdate") ? obj.get("messageUpdate").getAsString() : "";

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().info("A new version is available: " + latest);
                        if (!updateNotes.isEmpty()) plugin.getLogger().info("Update notes: " + updateNotes.replace("\n", " "));
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info("You are using the latest version (" + current + ")."));
                }
            }));
    }

    private String fetchJson() throws Exception {
        URL url = new URI(API_URL).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SimpleEconomy-Plugin");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        if (con.getResponseCode() != 200) return null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            return response.toString();
        } finally {
            con.disconnect();
        }
    }
}
