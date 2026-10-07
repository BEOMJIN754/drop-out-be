package com.dropout.demo;

import com.dropout.demo.domain.release.service.SupremeParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class SupremeParserTests {
    private final SupremeParser parser = new SupremeParser();
    private String html(String text) throws Exception {
        String flight = "1d:T" + Integer.toHexString(text.getBytes(StandardCharsets.UTF_8).length) + "," + text
                + "6:[\"$\",null,null,{\"newsItems\":[{\"_id\":\"test\",\"slug\":\"123\",\"title\":\"Test\","
                + "\"publishedAt\":\"2026-09-07T10:00:00Z\",\"body\":[{\"children\":[{\"text\":\"$1d\"}]}]}]}]";
        return "<script>self.__next_f.push(" + new ObjectMapper().writeValueAsString(new Object[]{1, flight}) + ")</script>";
    }
    @Test void usesAsiaDateAndResolvesUtf8FlightText() throws Exception {
        var item = parser.parse(html("Supreme® 의류. Available September 10th. Available in Asia September 12th.")).getFirst();
        assertEquals(LocalDate.of(2026, 9, 12), item.releaseDate());
        assertTrue(item.description().startsWith("Supreme® 의류."));
    }
    @Test void doesNotInventAsiaDateFromUsAnnouncement() throws Exception {
        assertNull(parser.parse(html("Available September 10th.")).getFirst().releaseDate());
    }
    @Test void ignoresEnclosingFlightJsonAfterNewsItems() throws Exception {
        var item = parser.parse(html("Text containing ] and \\\"escaped brackets [ ]\\\".")).getFirst();
        assertEquals("test", item.id());
        assertTrue(item.description().contains("escaped brackets [ ]"));
    }
    @Test void failsOnChangedPayloadRatherThanErasingData() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("<html>Unavailable</html>"));
    }
    @Test void parsesLiveCaptureWhenProvided() throws Exception {
        String fixture = System.getenv("SUPREME_CAPTURE");
        if (fixture == null) return;
        var items = parser.parse(Files.readString(Path.of(fixture)));
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(item -> item.releaseDate() != null));
        assertTrue(items.stream().noneMatch(item -> item.description().matches("\\$[0-9a-f]+")));
    }
}
