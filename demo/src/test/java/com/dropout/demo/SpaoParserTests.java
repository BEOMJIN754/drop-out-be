package com.dropout.demo;

import com.dropout.demo.domain.release.service.SpaoParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaoParserTests {
    private final SpaoParser parser = new SpaoParser();
    private String payload(String link) {
        return """
            {"resultCode":"200","data":{"banner":[{"cornerNo":"R13701004_mainCollabo",
            "contentsBannerName":"202609_콜라보_블리치_배너","contentsBannerTitleName":"",
            "subTitle2":"Hello &amp; friends&lt;br&gt;Collection",
            "image":[{"imageJoinUrl":"%s","imageFilePathName":"r/image/display/2026-09-03/test.jpg"}]}]}}
            """.formatted(link);
    }
    @Test void parsesOfficialLinksTextAndStableExhibitionId() throws Exception {
        var item = parser.parse(payload("/p/planshop?exhibitionNo=202609003210&amp;displayMallNo=0000137")).getFirst();
        assertEquals("spao-202609003210", item.id());
        assertEquals("SPAO · 블리치", item.title());
        assertEquals("Hello & friends Collection", item.description());
        assertTrue(item.sourceUrl().contains("&displayMallNo="));
        assertTrue(item.imageUrl().startsWith("https://spao-item.elandrs.com/"));
    }
    @Test void rejectsUntrustedLinksAndChangedPayload() {
        for (String link : new String[]{"https://evil.example/p/planshop?exhibitionNo=1", "javascript:alert(1)", "/p/planshop?other=1"})
            assertThrows(Exception.class, () -> parser.parse(payload(link)));
        assertThrows(Exception.class, () -> parser.parse("{\"resultCode\":\"500\"}"));
        assertThrows(Exception.class, () -> parser.parse("{\"resultCode\":\"200\",\"data\":{\"banner\":[]}}"));
    }
    @Test void parsesCaptureWhenProvided() throws Exception {
        String file = System.getenv("SPAO_CAPTURE");
        if (file == null) return;
        var items = parser.parse(java.nio.file.Files.readString(java.nio.file.Path.of(file)));
        assertFalse(items.isEmpty());
        assertEquals(items.size(), items.stream().map(SpaoParser.Item::id).distinct().count());
    }
}
