package com.dropout.demo.domain.release.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads public Next.js news payloads, including length-prefixed Flight text references. */
@Component
public class SupremeParser {
    private final ObjectMapper json = new ObjectMapper();
    public record Item(String id, String slug, String title, String description,
                       String imageAsset, OffsetDateTime publishedAt, LocalDate releaseDate) {}

    public List<Item> parse(String html) throws Exception {
        var chunks = Pattern.compile("self\\.__next_f\\.push\\((\\[.*?\\])\\)</script>", Pattern.DOTALL).matcher(html);
        var stream = new StringBuilder();
        while (chunks.find()) {
            JsonNode chunk = json.readTree(chunks.group(1));
            if (chunk.size() > 1 && chunk.get(1).isTextual()) stream.append(chunk.get(1).asText());
        }
        String flight = stream.toString();
        Map<String, String> texts = new HashMap<>();
        var references = Pattern.compile("(?:^|\\n)([0-9a-f]+):T([0-9a-f]+),").matcher(flight);
        while (references.find()) {
            byte[] remaining = flight.substring(references.end()).getBytes(StandardCharsets.UTF_8);
            int length = Integer.parseInt(references.group(2), 16);
            if (length > remaining.length) throw new IllegalArgumentException("Truncated news text");
            texts.put("$" + references.group(1), new String(remaining, 0, length, StandardCharsets.UTF_8));
        }
        int start = flight.indexOf("\"newsItems\":[");
        if (start < 0) throw new IllegalArgumentException("Supreme news payload changed");
        start += "\"newsItems\":".length();
        JsonNode items = json.readTree(jsonArrayAt(flight, start));
        List<Item> result = new ArrayList<>();
        for (JsonNode item : items) {
            var body = new StringBuilder();
            for (JsonNode block : item.path("body")) {
                for (JsonNode child : block.path("children")) {
                    String value = child.path("text").asText("");
                    if (value.matches("\\$[0-9a-f]+") && !texts.containsKey(value)) throw new IllegalArgumentException("Unresolved news text");
                    body.append(texts.getOrDefault(value, value));
                }
                body.append('\n');
            }
            String description = body.toString().trim();
            OffsetDateTime published = OffsetDateTime.parse(item.path("publishedAt").asText());
            // Only dates explicitly associated with Asia: never substitute the US date.
            var date = Pattern.compile("(?i)available in Asia[^.]*?([A-Z][a-z]+) (\\d{1,2})(?:st|nd|rd|th)?").matcher(description);
            LocalDate release = null;
            if (date.find()) {
                release = LocalDate.parse(date.group(1) + " " + date.group(2) + " " + published.getYear(),
                        DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH));
                if (release.isBefore(published.toLocalDate().minusMonths(6))) release = release.plusYears(1);
            }
            String id = item.path("_id").asText();
            String title = item.path("title").asText();
            String slug = item.path("slug").asText();
            if (id.isBlank() || title.isBlank() || !slug.matches("[a-zA-Z0-9-]+")) throw new IllegalArgumentException("Invalid news item");
            result.add(new Item(id, slug, title, description,
                    item.path("images").path(0).path("asset").path("_id").asText(""), published, release));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty Supreme news payload");
        return result;
    }

    /** Extracts one JSON array without consuming the enclosing React Flight record. */
    private String jsonArrayAt(String input, int start) {
        if (start >= input.length() || input.charAt(start) != '[')
            throw new IllegalArgumentException("Invalid Supreme news payload");
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < input.length(); index++) {
            char current = input.charAt(index);
            if (inString) {
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == '"') inString = false;
                continue;
            }
            if (current == '"') inString = true;
            else if (current == '[') depth++;
            else if (current == ']' && --depth == 0) return input.substring(start, index + 1);
        }
        throw new IllegalArgumentException("Truncated Supreme news payload");
    }
}
