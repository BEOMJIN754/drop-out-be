package com.dropout.demo.domain.brand.controller;

import com.dropout.demo.domain.brand.dto.BrandCreateRequest;
import com.dropout.demo.domain.brand.dto.BrandResponse;
import com.dropout.demo.domain.brand.service.BrandService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;

    @PostMapping("/create")
    public BrandResponse create(@RequestBody BrandCreateRequest request) {
        return brandService.create(request);
    }

    @GetMapping
    public List<BrandResponse> findAll() {
        return brandService.findAll();
    }

    @GetMapping("/{id}")
    public BrandResponse findById(@PathVariable(name = "id") Long id) {
        return brandService.findById(id);
    }
}
