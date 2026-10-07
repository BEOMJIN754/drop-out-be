package com.dropout.demo.domain.release.controller;

import com.dropout.demo.domain.release.dto.ReleaseCreateRequest;
import com.dropout.demo.domain.release.dto.ReleaseResponse;
import com.dropout.demo.domain.release.service.ReleaseService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/releases")
public class ReleaseController {
    private final ReleaseService releaseService;

    @PostMapping
    public ReleaseResponse create(@RequestBody ReleaseCreateRequest request) {
        return releaseService.create(request);
    }

    @GetMapping
    public List<ReleaseResponse> findAll(
            @RequestParam(name = "brand", required = false) Long brandId) {
        return brandId == null ? releaseService.findAll() : releaseService.findByBrand(brandId);
    }

    @GetMapping("/{id}")
    public ReleaseResponse findById(@PathVariable(name = "id") Long id) {
        return releaseService.findById(id);
    }
}
