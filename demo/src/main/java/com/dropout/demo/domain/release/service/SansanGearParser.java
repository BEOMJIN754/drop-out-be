package com.dropout.demo.domain.release.service;

import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SansanGearParser {
    public record Item(String id, String title, String description, String imageUrl, String logoUrl) {}

    public Item parse(String html) throws Exception {
        Document doc = Jsoup.parse(html, "https://sansangear.com/");
        String logo = null;
        for (var script : doc.select("script[type=application/ld+json]")) {
            var data = new ObjectMapper().readTree(script.data());
            if ("Organization".equals(data.path("@type").asText()) && "SAN SAN GEAR".equals(data.path("name").asText()))
                logo = data.path("logo").asText(null);
        }
        if (logo == null || !logo.startsWith("https://sansangear.com/cdn/shop/files/"))
            throw new IllegalArgumentException("Official Sansan Gear logo missing");
        for (var span : doc.select(".item .txt span")) {
            var match = Pattern.compile("^SAN SAN GEAR (FW|SS)(\\d{2}) - ['‘](.+?)['’] collection$", Pattern.CASE_INSENSITIVE).matcher(span.text());
            if (!match.matches()) continue;
            String season = match.group(1).toUpperCase(Locale.ROOT) + match.group(2);
            Element image = doc.selectFirst(".item img[src*=SAN-SAN-GEAR-" + season + "]");
            Element container = span.closest(".item");
            Element next = container == null ? null : container.nextElementSibling();
            Element description = next == null ? null : next.selectFirst(".txt span");
            if (image == null || description == null || !description.text().contains("collection")) continue;
            String url = image.absUrl("src");
            if (!url.startsWith("https://sansangear.com/cdn/shop/files/")) continue;
            // Dates in asset filenames and music release copy are not clothing release dates.
            return new Item("sansan-gear-" + season.toLowerCase(Locale.ROOT), span.text(), description.text(), url, logo);
        }
        throw new IllegalArgumentException("Sansan Gear collection page structure changed");
    }
}
