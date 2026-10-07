package com.dropout.demo.domain.follow;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandFollowRepository extends JpaRepository<BrandFollow, Long> {
    @Query(
            "select f.brand.id from BrandFollow f where f.user.id = :userId order by f.createdAt,"
                + " f.id")
    List<Long> brandIds(@Param("userId") Long userId);

    @Modifying
    @Query(
            value =
                    "insert into brand_follows(user_id, brand_id, created_at) values (:userId,"
                        + " :brandId, current_timestamp) on conflict (user_id, brand_id) do"
                        + " nothing",
            nativeQuery = true)
    void follow(@Param("userId") Long userId, @Param("brandId") Long brandId);

    void deleteByUser_IdAndBrand_Id(Long userId, Long brandId);
}
