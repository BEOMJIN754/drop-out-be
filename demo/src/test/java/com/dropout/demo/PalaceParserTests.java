package com.dropout.demo;

import com.dropout.demo.domain.release.service.PalaceParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PalaceParserTests {
    private final PalaceParser parser = new PalaceParser();
    private String html(String launch) throws Exception {
        var json = new ObjectMapper();
        return "<title>PALACE TEST | PALACE SKATEBOARDS</title><main><img src=\"https://www.datocms-assets.com/test.jpg\"></main>"
                + "<script>streamController.enqueue(" + json.writeValueAsString(json.writeValueAsString(new String[]{launch})) + ")</script>";
    }
    @Test void selectsKoreanSaturdayInsteadOfFridayUsTime() throws Exception {
        var item = parser.parse(html("LAUNCHING FRIDAY August 21st:\nUS online 11:00am EDT\nLAUNCHING SATURDAY August 22nd:\nJP 11:00am JST\nKR in-store 12:00pm KST"), "/advice/palace-test", 2026);
        assertEquals(OffsetDateTime.parse("2026-08-22T12:00:00+09:00"), item.releaseAt());
    }
    @Test void doesNotInventKoreanScheduleOrMismatchedYear() throws Exception {
        assertNull(parser.parse(html("LAUNCHING FRIDAY August 21st:\nUS 11:00am EDT"), "/advice/palace-test", 2026).releaseAt());
        assertNull(parser.parse(html("LAUNCHING SATURDAY August 22nd:\nKR 12:00pm KST"), "/advice/palace-test", 2027).releaseAt());
    }
    @Test void rejectsChangedPayload() {
        assertThrows(IllegalArgumentException.class, () -> parser.links("<html>error</html>"));
    }
    @Test void liveCapturedHomepageAndAnnouncement() throws Exception {
        String dir = System.getenv("PALACE_CAPTURE_DIR");
        org.junit.jupiter.api.Assumptions.assumeTrue(dir != null);
        String home = Files.readString(Path.of(dir, "dropout-palace.html"));
        assertTrue(parser.links(home).contains("/advice/palace-ebbets"));
        assertEquals(2026, parser.seasonYear(home));
        var item = parser.parse(Files.readString(Path.of(dir, "dropout-palace-ebbets.html")), "/advice/palace-ebbets", parser.seasonYear(home));
        assertEquals("PALACE EBBETS", item.title());
        assertEquals(OffsetDateTime.parse("2026-08-22T12:00:00+09:00"), item.releaseAt());
        assertTrue(item.imageUrl().startsWith("https://www.datocms-assets.com/"));
    }
}
