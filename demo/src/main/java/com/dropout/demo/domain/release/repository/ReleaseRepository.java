package com.dropout.demo.domain.release.repository;

import com.dropout.demo.domain.release.entity.Release;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    Optional<Release> findByBrandIdAndSourceUrlAndTitle(
            Long brandId, String sourceUrl, String title);

    List<Release> findByBrandId(Long brandId);
}
