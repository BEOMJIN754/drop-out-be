package com.dropout.demo.domain.brand.service;

import com.dropout.demo.domain.brand.dto.BrandCreateRequest;
import com.dropout.demo.domain.brand.dto.BrandResponse;
import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.entity.BrandCategory;
import com.dropout.demo.domain.brand.repository.BrandRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandResponse create(BrandCreateRequest request) {
        Brand brand =
                Brand.builder()
                        .name(request.name())
                        .origin(request.origin())
                        .foundedYear(request.foundedYear())
                        .category(
                                request.category() == null
                                        ? BrandCategory.OTHER
                                        : request.category())
                        .officialUrl(request.officialUrl())
                        .logoUrl(request.logoUrl())
                        .build();
        Brand savedBrand = brandRepository.save(brand);
        return BrandResponse.from(savedBrand);
    }

    public List<BrandResponse> findAll() {

        return brandRepository.findAll().stream().map(BrandResponse::from).toList();
    }

    public BrandResponse findById(Long id) {
        return BrandResponse.from(findEntityById(id));
    }

    public Brand findEntityById(Long id) {
        return brandRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("브랜드를 찾을 수 없습니다."));
    }
}
