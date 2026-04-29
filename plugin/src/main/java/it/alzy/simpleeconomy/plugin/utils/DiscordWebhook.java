package it.alzy.simpleeconomy.plugin.utils;

import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class DiscordWebhook {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String url;
    private final List<Embed> embeds = new ArrayList<>();
    @Setter
    private String content;
    @Setter
    private String username;
    @Setter
    private String avatarUrl;
    @Setter
    private boolean tts;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void addEmbed(Embed embed) {
        this.embeds.add(embed);
    }

    public CompletableFuture<Integer> sendAsync() {
        if (this.url == null || this.url.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("URL cannot be null"));
        }

        String jsonPayload = buildJson();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode);
    }


    private String buildJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        if (content != null) json.append("\"content\":").append(quote(content)).append(",");
        if (username != null) json.append("\"username\":").append(quote(username)).append(",");
        if (avatarUrl != null) json.append("\"avatar_url\":").append(quote(avatarUrl)).append(",");
        if (tts) json.append("\"tts\":true,");

        if (!embeds.isEmpty()) {
            json.append("\"embeds\":[");
            for (int i = 0; i < embeds.size(); i++) {
                Embed e = embeds.get(i);
                json.append("{");
                if (e.title != null) json.append("\"title\":").append(quote(e.title)).append(",");
                if (e.description != null) json.append("\"description\":").append(quote(e.description)).append(",");
                if (e.color != null) json.append("\"color\":").append(e.color).append(",");

                if (e.footerText != null) {
                    json.append("\"footer\":{\"text\":").append(quote(e.footerText));
                    if (e.footerIcon != null) json.append(",\"icon_url\":").append(quote(e.footerIcon));
                    json.append("},");
                }

                if (!e.fields.isEmpty()) {
                    json.append("\"fields\":[");
                    for (int j = 0; j < e.fields.size(); j++) {
                        Field f = e.fields.get(j);
                        json.append("{\"name\":").append(quote(f.name))
                                .append(",\"value\":").append(quote(f.value))
                                .append(",\"inline\":").append(f.inline).append("}");
                        if (j < e.fields.size() - 1) json.append(",");
                    }
                    json.append("],");
                }

                if (json.charAt(json.length() - 1) == ',') json.setLength(json.length() - 1);
                json.append("}");
                if (i < embeds.size() - 1) json.append(",");
            }
            json.append("],");
        }

        if (json.charAt(json.length() - 1) == ',') {
            json.setLength(json.length() - 1);
        }
        json.append("}");
        return json.toString();
    }

    private String quote(String string) {
        if (string == null || string.isEmpty()) return "\"\"";
        char c;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"', '/':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }


    public static class Embed {
        private final List<Field> fields = new ArrayList<>();
        private String title;
        private String description;
        private String color;
        private String footerText;
        private String footerIcon;

        public Embed setTitle(String title) {
            this.title = title;
            return this;
        }

        public Embed setDescription(String description) {
            this.description = description;
            return this;
        }

        public Embed setColor(int r, int g, int b) {
            this.color = String.valueOf((r << 16) + (g << 8) + b);
            return this;
        }
        public Embed setColor(String hex) {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            this.color = String.valueOf(Integer.parseInt(hex, 16));
            return this;
        }

        public Embed setFooter(String text, String iconUrl) {
            this.footerText = text;
            this.footerIcon = iconUrl;
            return this;
        }

        public Embed addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }
    }

    private static class Field {
        String name, value;
        boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}