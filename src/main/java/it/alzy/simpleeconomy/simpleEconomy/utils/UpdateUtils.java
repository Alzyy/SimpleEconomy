package it.alzy.simpleeconomy.simpleEconomy.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import it.alzy.simpleeconomy.simpleEconomy.SimpleEconomy;
import org.jetbrains.annotations.NotNull;

public class UpdateUtils {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    public UpdateUtils() {
    }

    private static final String API_URL = "https://api.spiget.org/v2/resources/127423/versions/latest";

    public void checkForUpdates() {
        try {
            String responseBody = getString();
            String latestVersion = extractValue(responseBody, "name");

            if (latestVersion == null) {
                plugin.getLogger().warning("⚠️ Could not parse latest version from Spiget API.");
                return;
            }

            if (!plugin.getPluginMeta().getVersion().equalsIgnoreCase(latestVersion)) {
                plugin.getLogger().warning("\n" +
                        "============================================\n" +
                        "  ⚠️  A new version of SimpleEconomy is available!\n" +
                        "  Current version : " + plugin.getPluginMeta().getVersion() + "\n" +
                        "  Latest version  : " + latestVersion + "\n" +
                        "  ➜ Download it at:\n" +
                        "    https://www.spigotmc.org/resources/simpleeconomy.127423/\n" +
                        "============================================");
            } else {
                plugin.getLogger().info("\n" +
                        "============================================\n" +
                        "   ✅ You are running the latest version of SimpleEconomy!\n" +
                        "         Thank you for keeping it updated! 🙌\n" +
                        "============================================");
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("⚠️ Error checking for updates: " + e.getMessage());
        }
    }

    private static @NotNull String getString() throws URISyntaxException, IOException {
        URI uri = new URI(API_URL);
        URL url = uri.toURL();

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

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

        String responseBody = response.toString();
        return responseBody;
    }


    public static String getLatestVersion() {
        String responseBody = null;
        try {
            responseBody = getString();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String latestVersion = extractValue(responseBody, "name");
        return latestVersion;
    }

    public static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1)
            return null;

        int colonIndex = json.indexOf(":", keyIndex);
        int firstQuote = json.indexOf("\"", colonIndex + 1);
        int secondQuote = json.indexOf("\"", firstQuote + 1);

        if (firstQuote == -1 || secondQuote == -1)
            return null;

        return json.substring(firstQuote + 1, secondQuote);
    }
}
