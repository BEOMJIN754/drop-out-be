package com.dropout.demo.domain.request;

import java.time.OffsetDateTime;
import java.util.List;

public final class BrandRequestDtos {
    private BrandRequestDtos() {}

    public record BrandRequestInput(String brandName, String officialUrl, String reason) {}

    public record BrandRequestReviewInput(RequestStatus status) {}

    public record BrandRequestResponse(
            Long id,
            String brandName,
            String officialUrl,
            String reason,
            String requestedBy,
            OffsetDateTime createdAt,
            RequestStatus status) {
        static BrandRequestResponse from(BrandRequest request) {
            return new BrandRequestResponse(
                    request.getId(),
                    request.getBrandName(),
                    request.getOfficialUrl(),
                    request.getReason(),
                    request.getUser().getUsername(),
                    request.getCreatedAt(),
                    request.getStatus());
        }
    }

    public record BrandRequestPage(List<BrandRequestResponse> items, boolean hasNext) {}
}
