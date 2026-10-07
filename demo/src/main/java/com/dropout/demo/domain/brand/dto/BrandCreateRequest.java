package com.dropout.demo.domain.brand.dto;

import com.dropout.demo.domain.brand.entity.BrandCategory;

public record BrandCreateRequest(
        String name,
        String origin,
        Integer foundedYear,
        BrandCategory category,
        String officialUrl,
        String logoUrl) {}
