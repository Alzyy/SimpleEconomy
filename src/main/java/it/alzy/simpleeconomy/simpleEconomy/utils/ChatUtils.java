package it.alzy.simpleeconomy.simpleEconomy.utils;

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

public class ChatUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private static final ConcurrentHashMap<String, Component> COMPONENT_CACHE = new ConcurrentHashMap<>(256);
    private static final ConcurrentHashMap<String, String> PLAIN_CACHE = new ConcurrentHashMap<>(128);
    private static final Component EMPTY_COMPONENT = Component.empty();

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
            component = LEGACY_SERIALIZER.deserialize(message);
        } catch (Exception e) {
            component = Component.text(message);
        }

        if (COMPONENT_CACHE.size() < 512) {
            COMPONENT_CACHE.put(message, component);
        }

        return component;
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
        sender.sendMessage(parse(processedMessage));
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(parse(message));
    }

    private static String applyPlaceholders(String message, Object... placeholders) {
        StringBuilder result = new StringBuilder(message);

        for (int i = 0; i < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);

            int index = 0;
            while ((index = result.indexOf(key, index)) != -1) {
                result.replace(index, index + key.length(), value);
                index += value.length();
            }
        }

        return result.toString();
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
    }

    public static String getCacheStats() {
        return String.format("ChatUtils Cache - Components: %d, Plain text: %d",
                COMPONENT_CACHE.size(), PLAIN_CACHE.size());
    }
}