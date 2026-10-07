package com.dropout.demo.domain.request;

import com.dropout.demo.domain.request.BrandRequestDtos.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brand")
public class BrandRequestController {
    private final BrandRequestService requestService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandRequestResponse create(@RequestBody BrandRequestInput input, Principal principal) {
        return requestService.create(input, principal);
    }

    @GetMapping("/requests")
    public BrandRequestPage mine(
            Principal principal, @RequestParam(name = "page", defaultValue = "0") int page) {
        return requestService.findByCurrentUser(principal, page);
    }

    @GetMapping("/admin/requests")
    public BrandRequestPage all(@RequestParam(name = "page", defaultValue = "0") int page) {
        return requestService.findAll(page);
    }

    @PatchMapping("/admin/requests/{id}/status")
    public BrandRequestResponse review(
            @PathVariable(name = "id") Long id, @RequestBody BrandRequestReviewInput input) {
        return requestService.review(id, input);
    }
}
