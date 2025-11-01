package it.alzy.simpleeconomy.plugin.utils;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UpdateUtils {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    private static final String API_URL = "https://ministats.alzy.site/api/v1/updates/check/3";

    @Getter
    private static boolean isUpdateAvailable = false;
    @Getter
    private static String updateNotes = "";
    @Getter
    private static String newVersion = "";

    public UpdateUtils() {
    }

    public void checkForUpdates() {
        try {
            String responseBody = getString();

            String latestVersion = extractValue(responseBody, "version");

            if (latestVersion == null) {
                plugin.getLogger().warning("⚠️ Could not parse latest version from API.");
                return;
            }

            String currentVersion = plugin.getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                UpdateUtils.isUpdateAvailable = true;
                UpdateUtils.newVersion = latestVersion;

                plugin.getLogger().info("A new version of SimpleEconomy is available! (Current: " + currentVersion + ", Latest: " + latestVersion + ")");

                String updateMessage = extractValue(responseBody, "messageUpdate");
                if (updateMessage != null && !updateMessage.isEmpty()) {
                    UpdateUtils.updateNotes = updateMessage.replace("\\n", "\n");
                    plugin.getLogger().info("Update notes: " + updateMessage.replace("\\n", " "));
                }

            } else {
                plugin.getLogger().info("You are using the latest version of SimpleEconomy (" + currentVersion + ").");
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("⚠️ Error checking for updates: " + e.getMessage());
        }
    }

    private static String getString() throws URISyntaxException, IOException {
        URI uri = new URI(API_URL);
        URL url = uri.toURL();

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SimpleExecutables-Plugin");

        int status = con.getResponseCode();
        InputStream inputStream;

        if (status >= 200 && status < 300) {
            inputStream = con.getInputStream();
        } else {
            inputStream = con.getErrorStream();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        in.close();
        con.disconnect();

        return response.toString();
    }


    public static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1)
            return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int firstQuote = json.indexOf("\"", colonIndex + 1);
        int secondQuote = json.indexOf("\"", firstQuote + 1);

        if (firstQuote == -1 || secondQuote == -1)
            return null;

        return json.substring(firstQuote + 1, secondQuote);
    }
}