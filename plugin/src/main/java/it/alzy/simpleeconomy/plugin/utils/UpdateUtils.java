package it.alzy.simpleeconomy.plugin.utils;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateUtils {

    private static final SimpleEconomy plugin = SimpleEconomy.getInstance();
    private static final String API_URL = "https://ministats.alzy.site/api/v1/updates/check/3";

    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"messageUpdate\"\\s*:\\s*\"([^\"]+)\"");

    @Getter private static volatile boolean isUpdateAvailable = false;
    @Getter private static volatile String updateNotes = "";
    @Getter private static volatile String newVersion = "";

    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String responseBody = fetchJson();
                if (responseBody == null) return;

                String latestVersion = extractValue(responseBody, VERSION_PATTERN);
                if (latestVersion == null) {
                    plugin.getLogger().warning("⚠️ Could not parse latest version from API.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    isUpdateAvailable = true;
                    newVersion = latestVersion;

                    plugin.getLogger().info("A new version of SimpleEconomy is available! (Current: " + currentVersion + ", Latest: " + latestVersion + ")");

                    String updateMessage = extractValue(responseBody, MESSAGE_PATTERN);
                    if (updateMessage != null && !updateMessage.isEmpty()) {
                        updateNotes = updateMessage.replace("\\n", "\n");
                        plugin.getLogger().info("Update notes: " + updateMessage.replace("\\n", " "));
                    }
                } else {
                    plugin.getLogger().info("You are using the latest version of SimpleEconomy (" + currentVersion + ").");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ Error checking for updates: " + e.getMessage());
            }
        }, plugin.getExecutor());
    }

    private String fetchJson() throws Exception {
        URL url = new URI(API_URL).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SimpleEconomy-Plugin");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        if (con.getResponseCode() != 200) {
            return null;
        }

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

    private static String extractValue(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}