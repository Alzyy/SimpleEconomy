package it.alzy.simpleeconomy.plugin.utils;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final ConcurrentHashMap<String, Component> COMPONENT_CACHE = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, String> CONVERSION_CACHE = new ConcurrentHashMap<>(256);
    private static final Component EMPTY_COMPONENT = Component.empty();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private static final String[] LEGACY_CODES = {"&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f", "&k", "&l", "&m", "&n", "&o", "&r"};
    private static final String[] MINI_TAGS = {"<black>", "<dark_blue>", "<dark_green>", "<dark_aqua>", "<dark_red>", "<dark_purple>", "<gold>", "<gray>", "<dark_gray>", "<blue>", "<green>", "<aqua>", "<red>", "<light_purple>", "<yellow>", "<white>", "<obfuscated>", "<bold>", "<strikethrough>", "<underlined>", "<italic>", "<reset>"};

    static {
        System.setProperty("adventure.minimessage.strict", "false");
    }

    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return EMPTY_COMPONENT;
        }

        Component cached = COMPONENT_CACHE.get(message);
        if (cached != null) {
            return cached;
        }

        Component component;
        try {
            String converted = convertLegacyToMiniMessage(message);
            component = MINI_MESSAGE.deserialize(converted);
        } catch (Exception e) {
            try {
                component = LEGACY_SERIALIZER.deserialize(message);
            } catch (Exception ex) {
                component = Component.text(message);
            }
        }

        if (COMPONENT_CACHE.size() < 512) {
            COMPONENT_CACHE.put(message, component);
        }

        return component;
    }

    private static String convertLegacyToMiniMessage(String message) {
        String cached = CONVERSION_CACHE.get(message);
        if (cached != null) {
            return cached;
        }

        StringBuilder result = new StringBuilder(message.length() + 32);

        Matcher matcher = HEX_PATTERN.matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(message, lastEnd, matcher.start());
            result.append("<color:#").append(matcher.group(1)).append('>');
            lastEnd = matcher.end();
        }
        result.append(message, lastEnd, message.length());

        String withHex = result.toString();

        if (withHex.indexOf('&') == -1) {
            if (CONVERSION_CACHE.size() < 512) {
                CONVERSION_CACHE.put(message, withHex);
            }
            return withHex;
        }

        String converted = withHex;
        for (int i = 0; i < LEGACY_CODES.length; i++) {
            if (converted.contains(LEGACY_CODES[i])) {
                converted = converted.replace(LEGACY_CODES[i], MINI_TAGS[i]);
            }
        }

        if (CONVERSION_CACHE.size() < 512) {
            CONVERSION_CACHE.put(message, converted);
        }

        return converted;
    }

    public static void send(CommandSender sender, String message, Object... placeholders) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        if (placeholders.length == 0) {
            sender.sendMessage(parse(message));
            return;
        }

        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be key-value pairs");
        }

        String processedMessage = applyPlaceholders(message, placeholders);
        Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
            if(!SimpleEconomy.getInstance().isPaper()) {
                SimpleEconomy.getInstance().getBukkitAudiences().sender(sender).sendMessage(parse(processedMessage));
            } else {
                sender.sendMessage(parse(processedMessage));
            }
        });
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        if(Bukkit.isPrimaryThread()) {
            sender.sendMessage(parse(message));
        } else {
            Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> sender.sendMessage(parse(message)));
        }
    }

    private static String applyPlaceholders(String message, Object... placeholders) {
        String result = message;

        for (int i = 0; i < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            result = result.replace(key, value);
        }

        return result;
    }

    public static void sendActionBar(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }
        if (!SimpleEconomy.getInstance().isPaper()) {
            SimpleEconomy.getInstance().getBukkitAudiences().player(player).sendActionBar(component);
        } else {
            player.sendActionBar(component);
        }
    }

    public static Component createComponent(String message, Object... placeholders) {
        if (message == null || message.isEmpty()) {
            return EMPTY_COMPONENT;
        }

        if (placeholders.length > 0) {
            if (placeholders.length % 2 != 0) {
                throw new IllegalArgumentException("Placeholders must be key-value pairs");
            }
            message = applyPlaceholders(message, placeholders);
        }

        return parse(message);
    }

    public static CompletableFuture<Component> createComponentAsync(String message, Object... placeholders) {
        return CompletableFuture.supplyAsync(() -> {
            if (message == null || message.isEmpty()) {
                return EMPTY_COMPONENT;
            }

            String processedMessage = message;

            if (placeholders.length > 0) {
                if (placeholders.length % 2 != 0) {
                    throw new IllegalArgumentException("Placeholders must be key-value pairs");
                }
                processedMessage = applyPlaceholders(processedMessage, placeholders);
            }

            return parse(processedMessage);
        });
    }

}