package com.dropout.demo.domain.follow;

import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.user.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandFollowService {
    private final BrandFollowRepository followRepository;
    private final BrandRepository brandRepository;
    private final CurrentUserService currentUserService;

    public List<Long> findAll(Principal principal) {
        return followRepository.brandIds(currentUserService.requireUser(principal).getId());
    }

    @Transactional
    public void follow(Long brandId, Principal principal) {
        Long userId = currentUserService.requireUser(principal).getId();
        if (!brandRepository.existsById(brandId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록되지 않은 브랜드입니다.");
        }
        followRepository.follow(userId, brandId);
    }

    @Transactional
    public void unfollow(Long brandId, Principal principal) {
        followRepository.deleteByUser_IdAndBrand_Id(
                currentUserService.requireUser(principal).getId(), brandId);
    }
}
