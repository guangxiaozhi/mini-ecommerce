package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.CreateReviewRequest;
import com.guang.miniecommercebackend.dto.UpdateReviewRequest;
import com.guang.miniecommercebackend.dto.ReviewResponse;
import com.guang.miniecommercebackend.dto.ReviewEligibilityResponse;
import com.guang.miniecommercebackend.dto.ReviewEligibilityResponse.ExistingReview;
import com.guang.miniecommercebackend.dto.ReviewEligibilityResponse.Reason;
import com.guang.miniecommercebackend.entity.OrderItem;
import com.guang.miniecommercebackend.entity.OrderStatus;
import com.guang.miniecommercebackend.entity.Product;
import com.guang.miniecommercebackend.entity.Review;
import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.repository.OrderItemRepository;
import com.guang.miniecommercebackend.repository.ProductRepository;
import com.guang.miniecommercebackend.repository.ReviewRepository;
import com.guang.miniecommercebackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
public class ReviewService {
    private static final  Set<OrderStatus> REVIEWABLE_STATUSES=EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CLOSED);
    private final  ReviewRepository reviewRepository;
    private final  OrderItemRepository orderItemRepository;
    private final  UserRepository userRepository;
    private final  ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderItemRepository orderItemRepository,
                         UserRepository userRepository, ProductRepository productRepository){
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public ReviewEligibilityResponse eligibility(String username, Long orderItemId){
        User user = requireUser(username);
        Optional<OrderItem> itemOpt = orderItemRepository.findById(orderItemId);
        if(itemOpt.isEmpty()){
            return new ReviewEligibilityResponse(false, ReviewEligibilityResponse.Reason.ORDER_ITEM_NOT_FOUND, null);
        }
        OrderItem item = itemOpt.get();
        if(!item.getOrder().getUserId().equals(user.getId())){
            return new ReviewEligibilityResponse(false, ReviewEligibilityResponse.Reason.NOT_OWNER, null);
        }
        if(!REVIEWABLE_STATUSES.contains(item.getOrder().getStatus())){
            return new ReviewEligibilityResponse(false, ReviewEligibilityResponse.Reason.ORDER_NOT_DELIVERED, null);
        }
        ExistingReview existing = reviewRepository.findByOrderItemId(orderItemId)
                .map(r->new ExistingReview(r.getId(), r.getRating(), r.getComment(),
                        r.getCreatedAt(), r.getUpdatedAt(), r.getDeletedAt(), r.isDeletedByAdmin())).orElse(null);
            return new ReviewEligibilityResponse(true, null, existing);
    }

    @Transactional
    public ReviewResponse create(String username, Long orderItemId, CreateReviewRequest req){
        User user = requireUser(username);
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_ITEM_NOT_FOUND"));
        if(!REVIEWABLE_STATUSES.contains(item.getOrder().getStatus())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDER_NOT_DELIVERED");
        }
        String comment = req.comment() == null? "":req.comment().strip();
        Optional<Review> existing = reviewRepository.findByOrderItemId(orderItemId);
        Review review;
        if(existing.isEmpty()){
            review = new Review();
            review.setOrderItemId(orderItemId);
            review.setUserId(user.getId());
            review.setProductID(item.getProductId());
        }else{
            Review prior = existing.get();
            if(prior.getDeletedAt() == null){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
            }
            if(prior.isDeletedByAdmin()){
                throw new ResponseStatusException((HttpStatus.CONFLICT, "REVIEW_HIDDEN_BY_ADMIN"));
            }
            //revive the user-soft-deleted row
            prior.setDeletedAt(null);
            review = prior;
        }
        review.setRating(req.rating());
        review.setComment(req.comment());
        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved, user.getUsername());
    }

    private ReviewResponse toReviewResponse(Review r, String username){
        boolean edited = !r.getCreatedAt().equals(r.getUpdatedAt());
        return new ReviewResponse(r.getId(), r.getRating(), r.getComment(), username,
                r.getCreatedAt(), r.getUpdatedAt(), edited, true);
    }

    @Transactional
    public ReviewResponse update(String username, Long reviewId, UpdateReviewRequest req){
        User user = requireUser(username);
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                ()->new ResponseStatusException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));
        if(!review.getUserId().equals(user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_OWNER");
        }
        if(!review.getDeletedAt() != null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REVIEW_DELETED");
        }
        if(req.rating() != null){
            review.setRating(req.rating());
        }
        if(req.comment() != null){
            review.setComment(req.comment().strip());
        }
        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved, user.getUsername());
    }

    @Transactional
    public void softDelete(String username, Long reviewId){
        User user = requireUser(username);
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                ()->new ResponseStatusException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));
        if(!review.getUserId().equals(user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NOT_OWNER");
        }
        if(review.getDeletedAt()!=null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REVIEW_DELETED");
        }
        review.setDeletedAt(java.time.LocalDateTime.now());
        review.setDeletedByAdmin(false);
        reviewRepository.save(review);
    }
}


























