package com.dropout.demo.domain.follow;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.user.entity.User;

import jakarta.persistence.*;

import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "brand_follows",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "brand_follows_user_id_brand_id_key",
                        columnNames = {"user_id", "brand_id"}),
        indexes = @Index(name = "idx_brand_follows_brand", columnList = "brand_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
