package com.dropout.demo.domain.brand.repository;

import com.dropout.demo.domain.brand.entity.Brand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByName(String name);
}
