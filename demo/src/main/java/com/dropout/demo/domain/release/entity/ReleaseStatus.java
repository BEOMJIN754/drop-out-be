package com.dropout.demo.domain.release.entity;

import java.time.*;

/** Derived on read: persisting this would leave upcoming releases stale as time passes. */
public enum ReleaseStatus {
    UPCOMING,
    RELEASED,
    UNKNOWN;

    public static ReleaseStatus from(OffsetDateTime releaseAt, LocalDate releaseDate, Clock clock) {
        if (releaseAt != null)
            return releaseAt.toInstant().isAfter(clock.instant()) ? UPCOMING : RELEASED;
        if (releaseDate != null)
            return releaseDate.isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))
                    ? RELEASED
                    : UPCOMING;
        return UNKNOWN;
    }
}
