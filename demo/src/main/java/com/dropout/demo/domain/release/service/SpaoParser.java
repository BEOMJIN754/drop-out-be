package com.dropout.demo.domain.release.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SpaoParser {
    public record Item(String id, String title, String description, String imageUrl, String sourceUrl) {}
    public List<Item> parse(String payload) throws Exception {
        var root = new ObjectMapper().readTree(payload);
        var banners = root.path("data").path("banner");
        if (!"200".equals(root.path("resultCode").asText()) || !banners.isArray())
            throw new IllegalArgumentException("SPAO collaboration payload changed");
        Map<String, Item> items = new LinkedHashMap<>();
        for (var banner : banners) {
            if (!"R13701004_mainCollabo".equals(banner.path("cornerNo").asText())) continue;
            String title = text(banner.path("contentsBannerTitleName").asText(""));
            if (title.isBlank()) title = text(banner.path("contentsBannerName").asText(""))
                    .replaceFirst("^(?:\\[NEW\\]\\s*)?\\d{6}_콜라보_", "").replaceFirst("_배너$", "");
            if (title.isBlank() || title.length() > 200) throw new IllegalArgumentException("Invalid SPAO title");
            for (var img : banner.path("image")) {
                String link = Parser.unescapeEntities(img.path("imageJoinUrl").asText(""), false);
                URI uri = URI.create("https://www.spao.com").resolve(link);
                if (!"https".equals(uri.getScheme()) || !"www.spao.com".equals(uri.getHost()) || uri.getUserInfo() != null
                        || !("/p/planshop".equals(uri.getPath()) || "/p/planshop-bo".equals(uri.getPath()))) continue;
                var id = Pattern.compile("(?:^|&)exhibitionNo=(\\d+)(?:&|$)").matcher(Objects.toString(uri.getQuery(), ""));
                if (!id.find()) continue;
                String image = img.path("imageFilePathName").asText("");
                if (!image.matches("r/image/display/[a-zA-Z0-9/_.-]+") || image.contains("..")) continue;
                String key = "spao-" + id.group(1);
                items.putIfAbsent(key, new Item(key, "SPAO · " + title,
                        text(banner.path("subTitle2").asText("")), "https://spao-item.elandrs.com/" + image, uri.toASCIIString()));
                break;
            }
        }
        if (items.isEmpty()) throw new IllegalArgumentException("No SPAO collaborations parsed");
        return List.copyOf(items.values());
    }
    private String text(String value) { return Jsoup.parse(Parser.unescapeEntities(value, false)).text().trim(); }
}
