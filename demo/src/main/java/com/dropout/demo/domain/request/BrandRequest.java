package com.dropout.demo.domain.request;

import com.dropout.demo.domain.user.entity.User;

import jakarta.persistence.*;

import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "brand_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandRequest {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'PENDING'")
    private RequestStatus status = RequestStatus.PENDING;

    public void review(RequestStatus status) {
        this.status = status;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brandName;

    @Column(nullable = false, length = 500)
    private String officialUrl;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public BrandRequest(String brandName, String officialUrl, String reason, User user) {
        this.brandName = brandName;
        this.officialUrl = officialUrl;
        this.reason = reason;
        this.user = user;
        this.createdAt = OffsetDateTime.now();
    }
}
