package it.alzy.simpleeconomy.plugin.utils;

import it.alzy.simpleeconomy.plugin.SimpleEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private static final ConcurrentHashMap<String, Component> COMPONENT_CACHE = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, String> PLAIN_CACHE = new ConcurrentHashMap<>(128);
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

    public static String removeColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        String cached = PLAIN_CACHE.get(message);
        if (cached != null) {
            return cached;
        }

        String plain = PLAIN_SERIALIZER.serialize(parse(message));

        if (PLAIN_CACHE.size() < 256) {
            PLAIN_CACHE.put(message, plain);
        }

        return plain;
    }

    public static void broadcast(String message, @Nullable String permission, @Nullable Object... placeholders) {
        if (message == null || message.isEmpty()) {
            return;
        }

        String processedMessage = (placeholders != null && placeholders.length > 0)
                ? applyPlaceholders(message, placeholders)
                : message;
        Component component = parse(processedMessage);

        if (permission == null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(component);
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(permission)) {
                    player.sendMessage(component);
                }
            }
        }
    }

    public static void broadcast(String message, @Nullable String permission) {
        broadcast(message, permission, (Object[]) null);
    }

    public static void broadcast(String message) {
        broadcast(message, null, (Object[]) null);
    }

    public static List<String> formatList(List<String> list, Object... placeholders) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        if (placeholders.length == 0) {
            return new ArrayList<>(list);
        }

        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be key-value pairs");
        }

        List<String> formatted = new ArrayList<>(list.size());
        for (String line : list) {
            if (line != null) {
                formatted.add(applyPlaceholders(line, placeholders));
            }
        }
        return formatted;
    }

    public static List<Component> colorList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<Component> colored = new ArrayList<>(list.size());
        for (String line : list) {
            if (line != null) {
                colored.add(parse(line));
            } else {
                colored.add(EMPTY_COMPONENT);
            }
        }
        return colored;
    }

    public static void sendList(CommandSender sender, List<String> messages, Object... placeholders) {
        if (sender == null || messages == null || messages.isEmpty()) {
            return;
        }

        for (String message : messages) {
            if (message != null) {
                send(sender, message, placeholders);
            }
        }
    }

    public static void sendComponentList(CommandSender sender, List<Component> components) {
        if (sender == null || components == null || components.isEmpty()) {
            return;
        }

        for (Component component : components) {
            if (component != null) {
                sender.sendMessage(component);
            }
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

    public static String stripColors(String message) {
        return removeColors(message);
    }

    public static boolean isValidMiniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return true;
        }

        try {
            MINI_MESSAGE.deserialize(message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearCache() {
        COMPONENT_CACHE.clear();
        PLAIN_CACHE.clear();
        CONVERSION_CACHE.clear();
    }

    public static String getCacheStats() {
        return String.format("ChatUtils Cache - Components: %d, Plain text: %d, Conversions: %d",
                COMPONENT_CACHE.size(), PLAIN_CACHE.size(), CONVERSION_CACHE.size());
    }
}