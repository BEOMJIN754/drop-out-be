package com.dropout.demo.domain.release.dto;

import com.dropout.demo.domain.release.entity.Release;
import com.dropout.demo.domain.release.entity.ReleaseStatus;
import com.dropout.demo.domain.release.entity.ReleaseType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReleaseResponse(
        Long id,
        BrandSummary brand,
        String title,
        String description,
        ReleaseType releaseType,
        OffsetDateTime releaseAt,
        LocalDate releaseDate,
        String dateLabel,
        ReleaseStatus status,
        String imageUrl,
        String sourceUrl,
        OffsetDateTime crawledAt) {
    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                new BrandSummary(release.getBrand().getId(), release.getBrand().getName()),
                release.getTitle(),
                release.getDescription(),
                release.getReleaseType(),
                release.getReleaseAt(),
                release.getReleaseDate(),
                release.getDateLabel(),
                ReleaseStatus.from(
                        release.getReleaseAt(), release.getReleaseDate(), Clock.systemUTC()),
                release.getImageUrl(),
                release.getSourceUrl(),
                release.getCrawledAt());
    }
}
