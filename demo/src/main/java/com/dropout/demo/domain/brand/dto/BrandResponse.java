package com.dropout.demo.domain.brand.dto;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.entity.BrandCategory;

public record BrandResponse(
        Long id,
        String name,
        String origin,
        Integer foundedYear,
        BrandCategory category,
        String officialUrl,
        String logoUrl) {
    public static BrandResponse from(Brand brand) {

        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getOrigin(),
                brand.getFoundedYear(),
                brand.getCategory(),
                brand.getOfficialUrl(),
                brand.getLogoUrl());
    }
}
