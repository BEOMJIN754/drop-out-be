package com.dropout.demo.domain.release.service;

import com.dropout.demo.domain.brand.service.BrandService;
import com.dropout.demo.domain.release.dto.ReleaseCreateRequest;
import com.dropout.demo.domain.release.dto.ReleaseResponse;
import com.dropout.demo.domain.release.entity.Release;
import com.dropout.demo.domain.release.repository.ReleaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseService {
    private final ReleaseRepository releaseRepository;
    private final BrandService brandService;

    @Transactional
    public ReleaseResponse create(ReleaseCreateRequest request) {
        Release release =
                Release.builder()
                        .brand(brandService.findEntityById(request.brandId()))
                        .title(request.title())
                        .description(request.description())
                        .releaseType(request.releaseType())
                        .releaseAt(request.releaseAt())
                        .releaseDate(request.releaseDate())
                        .dateLabel(request.dateLabel())
                        .imageUrl(request.imageUrl())
                        .sourceUrl(request.sourceUrl())
                        .crawledAt(request.crawledAt())
                        .build();
        return ReleaseResponse.from(releaseRepository.save(release));
    }

    public List<ReleaseResponse> findAll() {
        return releaseRepository.findAll().stream().map(ReleaseResponse::from).toList();
    }

    public ReleaseResponse findById(Long id) {
        Release release =
                releaseRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("릴리스를 찾을 수 없습니다."));
        return ReleaseResponse.from(release);
    }

    public List<ReleaseResponse> findByBrand(Long brandId) {
        return releaseRepository.findByBrandId(brandId).stream()
                .map(ReleaseResponse::from)
                .toList();
    }
}
