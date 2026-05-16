package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.*;
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
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

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
            review.setProductId(item.getProductId());
        }else{
            Review prior = existing.get();
            if(prior.getDeletedAt() == null){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
            }
            if(prior.isDeletedByAdmin()){
                throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_HIDDEN_BY_ADMIN");
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
        if(review.getDeletedAt() != null){
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

    @Transactional
    public void adminHide(Long reviewId){
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                ()->new ResponseStatusException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));
        if(review.getDeletedAt() != null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_ALREADY_DELETED");
        }
        review.setDeletedAt(java.time.LocalDateTime.now());
        review.setDeletedByAdmin(true);
        reviewRepository.save(review);
    }

    @Transactional
    public void adminUnhide(Long reviewId){
        Review review  =reviewRepository.findById(reviewId).orElseThrow(
                ()->new ResponseStatusException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));
        if(review.getDeletedAt() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "REVIEW_NOT_DELETED");
        }
        if (!review.isDeletedByAdmin()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "NOT_ADMIN_HIDDEN");
        }
        review.setDeletedAt(null);
        review.setDeletedByAdmin(false);
        reviewRepository.save(review);
    }

    public Page<ReviewResponse> listForProduct(Long productId, int page, int size, String sort){
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), parseSort(sort));
        Page<Review> reviews = reviewRepository.findActiveByProductId(productId, pageable);

        //resolve usernames in one query
        Set<Long> userIds = reviews.getContent().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernames = userRepository.findAllById(userIds).stream().collect(
                Collectors.toMap(User::getId, User::getUsername));
        return reviews.map(r->toReviewResponse(r, usernames.getOrDefault(r.getUserId(), "")));
    }

    public Page<MyReviewResponse> listForUser(String username, int page, int size, String sort){
        User user = requireUser(username);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), parseSort(sort));
        Page<Review> reviews = reviewRepository.findVisibleToUser(user.getId(), pageable);
        Set<Long> productIds = reviews.getContent().stream()
                .map(Review::getProductId).collect(Collectors.toSet());
        Map<Long, String> productNames = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        return reviews.map(r->{
            boolean edited = !r.getCreatedAt().equals(r.getUpdatedAt());
            return new MyReviewResponse(r.getId(), r.getProductId(), productNames.getOrDefault(r.getProductId(), ""),
                    r.getRating(), r.getComment(), r.getCreatedAt(), r.getUpdatedAt(), edited, r.isDeletedByAdmin());
        });
    }

    public Map<Long, RatingSummary> summariesFor(Collection<Long> productIds){
        if(productIds == null || productIds.isEmpty()){
            return Collections.emptyMap();
        }
        return reviewRepository.aggregateForProductIds(productIds).stream().collect(Collectors.toMap(RatingSummary::getProductId, s->s));
    }

    private Sort parseSort(String sort){
        return switch (sort == null? "newest":sort){
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"));
            case "highest"-> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
            case "lowest" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SORT");
        };
    }

    public Page<AdminReviewResponse> listForAdmin(String productName, String username, boolean hiddenOnly,
                                                  int page, int size, String sort){
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), parseSort(sort));
        Collection<Long> productIdFilter = null;
        if(productName != null && !productName.isBlank()){
            productIdFilter = productRepository.findByNameContainingIgnoreCase(productName.trim())
                    .stream().map(Product::getId).collect(Collectors.toList());
            if(productIdFilter.isEmpty()) return Page.empty(pageable);
        }
        Collection<Long> userIdFilter = null;
        if(username != null && !username.isBlank()){
            userIdFilter = userRepository.findByUsernameContainingIgnoreCase(username.trim())
                    .stream().map(User::getId).collect(Collectors.toList());
            if(userIdFilter.isEmpty()) return Page.empty(pageable);
        }
        boolean allProducts = (productIdFilter == null);
        boolean allUsers = (userIdFilter == null);
        Collection<Long> productIds = productIdFilter != null ? productIdFilter : java.util.List.of(-1L);
        Collection<Long> userIds = userIdFilter != null ? userIdFilter : java.util.List.of(-1L);
        Page<Review> reviews = reviewRepository.findForAdmin(allProducts, productIds, allUsers, userIds, hiddenOnly, pageable);

        Set<Long> resultProductIds = reviews.getContent().stream().map(Review::getProductId).collect(Collectors.toSet());
        Set<Long> resultUserIds = reviews.getContent().stream().map(Review::getUserId).collect(Collectors.toSet());
        Map<Long, String> productNames = productRepository.findAllById(resultProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        Map<Long, String> usernames = userRepository.findAllById(resultUserIds).stream().collect(Collectors.toMap(User::getId, User::getUsername));
        return reviews.map(r->{
            AdminReviewResponse.Status status;
            if(r.getDeletedAt() == null){
                status = AdminReviewResponse.Status.ACTIVE;
            }else if(r.isDeletedByAdmin()){
                status = AdminReviewResponse.Status.HIDDEN_BY_ADMIN;
            }else{
                status = AdminReviewResponse.Status.DELETED_BY_USER;
            }
            return new AdminReviewResponse(r.getId(), r.getProductId(), productNames.getOrDefault(r.getProductId(), ""),
                    r.getUserId(), usernames.getOrDefault(r.getUserId(), ""), r.getRating(), r.getComment(),
                    r.getCreatedAt(), r.getUpdatedAt(), status);
        });
    }

    private User requireUser(String username){
        return  userRepository.findByUsername(username)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
    }

}


























