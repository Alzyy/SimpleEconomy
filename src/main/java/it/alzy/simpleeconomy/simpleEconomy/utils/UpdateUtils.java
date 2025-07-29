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

public class UpdateUtils {
    
    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    public UpdateUtils() {}

    final String API_URL = "https://api.spigotmc.org/legacy/update.php?resource=127423";

    public void checkForUpdates()  {
        try {
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
                response.append(line).append("\n");
            }

            in.close();
            con.disconnect();

            String responseBody = response.toString();
            if (!plugin.getPluginMeta().getVersion().equals(responseBody)) {
                plugin.getLogger().warning("\n" +
                        "============================================\n" +
                        "  ⚠️  A new version of SimpleEconomy is available!\n" +
                        "  Current version : " + plugin.getPluginMeta().getVersion() + "\n" +
                        "  Latest version  : " + responseBody.trim() + "\n" +
                        "  ➜ Download it at:\n" +
                        "    https://www.spigotmc.org/resources/simpleeconomy.127423/\n" +
                        "============================================");
            }
        } catch(IOException | URISyntaxException e) {
            
        }
    }


}
