package com.dropout.demo.domain.request;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRequestRepository extends JpaRepository<BrandRequest, Long> {
    Page<BrandRequest> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
