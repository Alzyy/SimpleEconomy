package it.alzy.simpleeconomy.plugin.logging;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.DiscordWebhook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class WebhookLogger {

    public void send(String type, String p1, String p2, double amount) {
        SettingsConfig config = SettingsConfig.getInstance();
        String template = type.toLowerCase().split(" ")[0];

        if (!config.shouldLogToDiscord()) {
            return;
        }

        String url = config.webhookURL();
        if (url == null || url.isEmpty() || url.contains("your_webhook_url")) {
            SimpleEconomy.getInstance().getLogger().warning("Webhook URL is invalid or not configured.");
            return;
        }

        Map<String, String> embedTemplate = config.getEmbedTemplate(template);

        if (embedTemplate == null || embedTemplate.isEmpty()) {
            SimpleEconomy.getInstance().getLogger().warning("No template found for type: " + type);
        }

        String formattedAmount = SimpleEconomy.getInstance().getFormatUtils().formatBalance(amount);

        if (embedTemplate == null || embedTemplate.isEmpty()) {
            SimpleEconomy.getInstance().getLogger().warning("No template found for type: " + type);
            return;
        }
        String title = embedTemplate.getOrDefault("title", "Transaction Log");
        String desc = embedTemplate.getOrDefault("description", "")
                .replace("%sender%", p1)
                .replace("%receiver%", p2)
                .replace("%executor%", p2)
                .replace("%target%", p1)
                .replace("%player%", p1)
                .replace("%amount%", formattedAmount);

        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setUsername(config.username());
        webhook.setAvatarUrl(config.avatarURL());

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed();
        embed.setTitle(title);
        embed.setDescription(desc);
        embed.setColor(embedTemplate.getOrDefault("color", config.webhookColor()));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
        embed.setFooter(config.embedFooter().replace("%timestamp%", time), null);

        webhook.addEmbed(embed);

        webhook.sendAsync().thenAccept(code -> {
            if (code < 200 || code >= 300) {
                SimpleEconomy.getInstance().getLogger().warning("Webhook failed! HTTP Code: " + code);
            }
        }).exceptionally(ex -> {
            SimpleEconomy.getInstance().getLogger().severe("Exception during webhook send: " + ex.getMessage());
            return null;
        });
    }
}