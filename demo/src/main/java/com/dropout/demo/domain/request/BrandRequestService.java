package com.dropout.demo.domain.request;

import com.dropout.demo.domain.request.BrandRequestDtos.*;
import com.dropout.demo.domain.user.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.Principal;

@Service
@RequiredArgsConstructor
public class BrandRequestService {
    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGE = 100_000;
    private final BrandRequestRepository requestRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public BrandRequestResponse create(BrandRequestInput input, Principal principal) {
        String name = validateText(input.brandName(), 100, true);
        String url = validateText(input.officialUrl(), 500, false);
        String reason = validateText(input.reason(), 2000, false);
        if (!url.isEmpty()) {
            try {
                URI uri = URI.create(url);
                if (!("https".equalsIgnoreCase(uri.getScheme())
                                || "http".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException error) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공식 사이트 주소를 확인해주세요.");
            }
        }
        return BrandRequestResponse.from(
                requestRepository.save(
                        new BrandRequest(
                                name, url, reason, currentUserService.requireUser(principal))));
    }

    @Transactional(readOnly = true)
    public BrandRequestPage findByCurrentUser(Principal principal, int page) {
        var result =
                requestRepository.findByUser_IdOrderByCreatedAtDescIdDesc(
                        currentUserService.requireUser(principal).getId(), pageable(page));
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public BrandRequestPage findAll(int page) {
        var result = requestRepository.findAll(pageable(page));
        return toPage(result);
    }

    @Transactional
    public BrandRequestResponse review(Long id, BrandRequestReviewInput input) {
        if (input.status() != RequestStatus.APPROVED && input.status() != RequestStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 또는 거절 상태를 선택해주세요.");
        var request =
                requestRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        request.review(input.status());
        return BrandRequestResponse.from(requestRepository.save(request));
    }

    private BrandRequestPage toPage(Page<BrandRequest> page) {
        return new BrandRequestPage(
                page.getContent().stream().map(BrandRequestResponse::from).toList(),
                page.hasNext());
    }

    private Pageable pageable(int page) {
        if (page < 0 || page > MAX_PAGE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private String validateText(String value, int max, boolean required) {
        String text = value == null ? "" : value.trim();
        if (text.length() > max || (required && text.isEmpty()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입력 내용을 확인해주세요.");
        return text;
    }
}
