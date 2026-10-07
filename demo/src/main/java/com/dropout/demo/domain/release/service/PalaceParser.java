package com.dropout.demo.domain.release.service;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@Component
public class PalaceParser {
    private final ObjectMapper json = new ObjectMapper();
    public record Item(String slug, String title, String description, String imageUrl, OffsetDateTime releaseAt) {}

    // React Router serializes string values in an indexed array. Read strings without executing scripts.
    List<String> strings(String html) throws Exception {
        List<String> values = new ArrayList<>();
        String marker = "streamController.enqueue(";
        int cursor = 0;
        while ((cursor = html.indexOf(marker, cursor)) >= 0) {
            cursor += marker.length();
            // Jackson reads one JSON value, avoiding recursive regex matching on large payloads.
            // The enclosing JavaScript is not JSON; consume only its first string token.
            String payload;
            try (var stream = json.createParser(html.substring(cursor))) {
                if (stream.nextToken() != tools.jackson.core.JsonToken.VALUE_STRING)
                    throw new IllegalArgumentException("Palace stream payload must be a string");
                payload = stream.getString();
            }
            var array = json.readTree(payload);
            for (var node : array) if (node.isTextual()) values.add(node.asText());
        }
        if (values.isEmpty()) throw new IllegalArgumentException("Palace payload changed");
        return values;
    }

    public List<String> links(String html) throws Exception {
        return strings(html).stream().filter(s -> s.matches("/advice/palace-[a-z0-9-]+"))
                .distinct().limit(8).toList();
    }

    public int seasonYear(String html) throws Exception {
        for (String value : strings(html)) {
            var match = Pattern.compile("/ranges/[a-z]+-(20\\d{2})").matcher(value);
            if (match.matches()) return Integer.parseInt(match.group(1));
        }
        throw new IllegalArgumentException("Palace active season year missing");
    }

    public Item parse(String html, String path, int year) throws Exception {
        var doc = Jsoup.parse(html, "https://palaceskateboards.com");
        String title = doc.title().split("\\|", 2)[0].trim();
        var image = doc.selectFirst("main img[src^=https://www.datocms-assets.com/], main img[src^=https://cdn.shopify.com/]");
        if (!path.matches("/advice/palace-[a-z0-9-]+") || !title.startsWith("PALACE ") || image == null)
            throw new IllegalArgumentException("Invalid Palace announcement");
        String description = strings(html).stream().filter(s -> s.contains("LAUNCHING ")).findFirst().orElse("");
        // Missing launch information is not a drop announcement (e.g. general advice).
        if (description.isBlank()) throw new IllegalArgumentException("Palace launch information missing");
        OffsetDateTime release = null;
        var blocks = Pattern.compile("(?is)LAUNCHING\\s+(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)\\s+([a-z]+)\\s+(\\d{1,2})(?:st|nd|rd|th)?:(.*?)(?=LAUNCHING|$)").matcher(description);
        while (blocks.find()) {
            var time = Pattern.compile("(?i)KR[^\\n]*?(\\d{1,2}):(\\d{2})(am|pm)\\s+KST").matcher(blocks.group(4));
            if (!time.find()) continue;
            LocalDate date = LocalDate.parse(blocks.group(2) + " " + blocks.group(3) + " " + year,
                    new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d uuuu").toFormatter(Locale.ENGLISH));
            // Year is inferred from the active season; weekday must agree with the official announcement.
            if (!date.getDayOfWeek().name().equalsIgnoreCase(blocks.group(1))) continue;
            int hour = Integer.parseInt(time.group(1));
            if (hour < 1 || hour > 12) throw new IllegalArgumentException("Invalid launch hour");
            hour = hour % 12 + (time.group(3).equalsIgnoreCase("pm") ? 12 : 0);
            release = date.atTime(hour, Integer.parseInt(time.group(2))).atOffset(ZoneOffset.ofHours(9));
        }
        String imageUrl = image.absUrl("src");
        if (!imageUrl.startsWith("https://www.datocms-assets.com/") && !imageUrl.startsWith("https://cdn.shopify.com/"))
            throw new IllegalArgumentException("Unexpected Palace image host");
        return new Item(path.substring("/advice/".length()), title, description, imageUrl, release);
    }
}
