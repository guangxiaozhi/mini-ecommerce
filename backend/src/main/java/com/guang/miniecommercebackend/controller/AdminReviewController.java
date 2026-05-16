package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.AdminReviewResponse;
import com.guang.miniecommercebackend.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
    private final ReviewService reviewService;
    public AdminReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('REVIEW_MODERATE'))")
    @GetMapping
    public Page<AdminReviewResponse> list(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "false") boolean hiddenOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10")int size,
            @RequestParam(defaultValue = "newest") String sort){
        return reviewService.listForAdmin(productName, username, hiddenOnly, page, size, sort);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('REVIEW_MODERATE'))")
    @PostMapping("/{reviewId}/unhide")
    public ResponseEntity<Void> unhide(@PathVariable Long reviewId){
        reviewService.adminUnhide(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('REVIEW_MODERATE'))")
    @PostMapping("/{reviewId}/hide")
    public ResponseEntity<Void> hide(@PathVariable Long reviewId){
        reviewService.adminHide(reviewId);
        return ResponseEntity.noContent().build();
    }
}
