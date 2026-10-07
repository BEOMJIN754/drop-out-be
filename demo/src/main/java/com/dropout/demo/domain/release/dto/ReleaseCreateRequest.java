package com.dropout.demo.domain.release.dto;

import com.dropout.demo.domain.release.entity.ReleaseType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReleaseCreateRequest(
        Long brandId,
        String title,
        String description,
        ReleaseType releaseType,
        OffsetDateTime releaseAt,
        LocalDate releaseDate,
        String dateLabel,
        String imageUrl,
        String sourceUrl,
        OffsetDateTime crawledAt) {}
