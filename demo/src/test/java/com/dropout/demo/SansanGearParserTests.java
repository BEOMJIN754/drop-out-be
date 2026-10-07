package com.dropout.demo;

import com.dropout.demo.domain.release.service.SansanGearParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SansanGearParserTests {
    private final SansanGearParser parser = new SansanGearParser();
    @Test void extractsOfficialLogoAndCollection() throws Exception {
        String html = """
            <script type="application/ld+json">{"@type":"Organization","name":"SAN SAN GEAR","logo":"https://sansangear.com/cdn/shop/files/logo.svg"}</script>
            <div class="item"><img src="//sansangear.com/cdn/shop/files/SAN-SAN-GEAR-FW26-first-release-2026.08.24.jpg"></div>
            <div class="item"><div class="txt"><span>SAN SAN GEAR FW26 - ‘CABLE’ collection</span></div></div>
            <div class="item"><div class="txt"><span>Our new collection.</span></div></div>
            <div class="item"><div class="txt"><span>Album release at 6 PM.</span></div></div>
            """;
        var item = parser.parse(html);
        assertEquals("sansan-gear-fw26", item.id());
        assertEquals("Our new collection.", item.description());
        assertEquals("https://sansangear.com/cdn/shop/files/logo.svg", item.logoUrl());
        assertTrue(item.imageUrl().startsWith("https://"));
    }
    @Test void rejectsMissingSourceData() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("<html>Unavailable</html>"));
    }
    @Test void parsesLiveCapture() throws Exception {
        String file = System.getenv("SANSAN_CAPTURE");
        org.junit.jupiter.api.Assumptions.assumeTrue(file != null);
        var item = parser.parse(Files.readString(Path.of(file)));
        assertEquals("sansan-gear-fw26", item.id());
        assertTrue(item.title().contains("CABLE"));
        assertTrue(item.logoUrl().contains("logo.svg"));
        assertFalse(item.description().contains("album"));
    }
}
