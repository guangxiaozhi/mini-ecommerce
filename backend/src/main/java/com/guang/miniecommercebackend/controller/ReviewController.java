package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.CreateReviewRequest;
import com.guang.miniecommercebackend.dto.MyReviewResponse;
import com.guang.miniecommercebackend.dto.ReviewEligibilityResponse;
import com.guang.miniecommercebackend.dto.ReviewResponse;
import com.guang.miniecommercebackend.dto.UpdateReviewRequest;
import com.guang.miniecommercebackend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {
    private final  ReviewService reviewService;
    public ReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }

    //Public-list reviews for a product
    @GetMapping("/api/products/{productId}/reviews")
    public  Page<ReviewResponse> listForProduct(@PathVariable Long productId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(defaultValue = "newest") String sort){
        return reviewService.listForProduct(productId, page, size, sort);
    }

    //Author - eligibility check for a specific OrderItem
    @GetMapping("/api/order-items/{orderItemId}/review/eligibility")
    public ReviewEligibilityResponse eligibility(Authentication auth, @PathVariable Long orderItemId){
        String username = (String) auth.getPrincipal();
        return reviewService.eligibility(username, orderItemId);
    }

    //Author - create a review on an OrderItem
    @PostMapping("/api/order-items/{orderItemId}/review")
    public ResponseEntity<ReviewResponse> create(Authentication auth,
                                                 @PathVariable Long orderItemId,
                                                 @Valid @RequestBody CreateReviewRequest req){
        String username = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(username, orderItemId, req));
    }

    //Author - edit own review
    @PatchMapping("/api/reviews/{reviewId}")
    public ReviewResponse update(Authentication auth,
                                 @PathVariable Long reviewId,
                                 @Valid @RequestBody UpdateReviewRequest req){
        String username = (String) auth.getPrincipal();
        return reviewService.update(username, reviewId, req);
    }

    //Author - soft-delete own review
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long reviewId){
        String username = (String) auth.getPrincipal();
        reviewService.softDelete(username, reviewId);
        return ResponseEntity.noContent().build();
    }

    //Author - list my reviews
    @GetMapping("/api/account/reviews")
    public Page<MyReviewResponse> listMine(Authentication auth,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(defaultValue = "newest") String sort){
        String username = (String) auth.getPrincipal();
        return reviewService.listForUser(username, page, size, sort);
    }
}





















