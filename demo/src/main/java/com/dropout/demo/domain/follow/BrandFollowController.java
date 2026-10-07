package com.dropout.demo.domain.follow;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/follows")
public class BrandFollowController {
    private final BrandFollowService followService;

    @GetMapping
    public List<Long> list(Principal principal) {
        return followService.findAll(principal);
    }

    @PutMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable(name = "brandId") Long brandId, Principal principal) {
        followService.follow(brandId, principal);
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable(name = "brandId") Long brandId, Principal principal) {
        followService.unfollow(brandId, principal);
    }
}
