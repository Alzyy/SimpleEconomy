package it.alzy.simpleeconomy.plugin.logging;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import it.alzy.simpleeconomy.plugin.configurations.SettingsConfig;
import it.alzy.simpleeconomy.plugin.utils.DiscordWebhook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WebhookLogger {
    public void send(String type, String p1, String p2, double amount) {
        SettingsConfig config = SettingsConfig.getInstance();

        if (!config.shouldLogToDiscord()) {
            return;
        }

        boolean enabled = switch (type.toLowerCase()) {
            case "pay" -> config.logPayToDiscord();
            case "remove" -> config.logWithdrawalsToDiscord();
            case "give", "set" -> config.logAdminToDiscord();
            case "withdraw" -> config.logVoucherCreations();
            default -> false;
        };

        if (!enabled) return;

        String url = config.webhookURL();
        if (url == null || url.isEmpty() || url.contains("your_webhook_url")) {
            return;
        }

        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setUsername(config.username());
        webhook.setAvatarUrl(config.avatarURL());

        DiscordWebhook.Embed embed = new DiscordWebhook.Embed();
        embed.setColor(config.webhookColor());

        String title = "💰 Transaction Log";
        String description = "";

        switch (type.toLowerCase()) {
            case "pay" -> {
                title = "💸 Player Payment";
                description = "**" + p1 + "** sent money to **" + p2 + "**";
                embed.addField("Sender", p1, true);
                embed.addField("Receiver", p2, true);
            }
            case "remove" -> {
                title = "📉 Admin Action: Remove";
                description = "Admin **" + p2 + "** removed money from **" + p1 + "**";
                embed.addField("Admin/Executor", p2, true);
                embed.addField("Target Player", p1, true);
            }
            case "give" -> {
                title = "📈 Admin Action: Give";
                description = "Admin **" + p2 + "** gave money to **" + p1 + "**";
                embed.addField("Admin/Executor", p2, true);
                embed.addField("Target Player", p1, true);
            }
            case "set" -> {
                title = "⚙️ Admin Action: Set";
                description = "Admin **" + p2 + "** set the balance for **" + p1 + "**";
                embed.addField("Admin/Executor", p2, true);
                embed.addField("Target Player", p1, true);
            }
            case "withdraw" -> {
                title = "🎫 Voucher Creation";
                description = "**" + p1 + "** created a physical voucher";
                embed.addField("Player", p1, true);
            }
        }

        embed.setTitle(title);
        embed.setDescription(description);

        embed.addField("Amount", SimpleEconomy.getInstance().getFormatUtils().formatBalance(amount), false);

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
        embed.setFooter("SimpleEconomy Audit Log • " + time, null);

        webhook.addEmbed(embed);

        webhook.sendAsync().thenAccept(code -> {
            if (code < 200 || code >= 300) {
                SimpleEconomy.getInstance().getLogger().warning("Discord Webhook dispatch failed. HTTP Code: " + code);
            }
        }).exceptionally(ex -> {
            SimpleEconomy.getInstance().getLogger().severe("Exception during Discord logging: " + ex.getMessage());
            return null;
        });
    }
}