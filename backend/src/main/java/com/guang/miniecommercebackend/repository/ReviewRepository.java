package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.dto.RatingSummary;
import com.guang.miniecommercebackend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>{
    /** Active(non-deleted) review for a given OrderItem, if any. */
    @Query("select r from Review r where r.orderItemId = :orderItemId and r.deletedAt is null")
    Optional<Review> findActiveByOrderItemId(Long orderItemId);

    /**Any review row for a given OrderItem(active or soft-deleted). Used by the eligibility endpoint and create-revive.*/
    Optional<Review> findByOrderItemId(Long orderItemId);

    /**Any reviews for a product. Use with Pageable carrying a Sort.*/
    @Query("select r from Review r where r.productId = :productId and r.deletedAt is null")
    Page<Review> findActiveByProductId(@Param("productId") Long productId, Pageable pageable);

    /**"Visible to user" = own active reviews PLUS own admin-hidden reviews. Excludes user-soft-deleted rows.*/
    @Query("""
        select r from Review r where r.userId = :userId and (r.deletedAt is null or r.deletedByAdmin = true)
    """)
    Page<Review> findVisibleToUser(@Param("userId") Long userId, Pageable pageable);

    /**Aggregate(avg rating, count) for a set of products, only counting active reviews. */
    @Query("""
        select new com.guang.miniecommercebackend.dto.RatingSummary(
            r.productId, avg(cast(r.rating as double)), count(r))
        from Review r
        where r.deletedAt is null and r.productId in :productIds group by r.productId
    """)
    List<RatingSummary> aggregateForProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("""
        select r from Review r
        where (:allProducts = true or r.productId in :productIds)
          and (:allUsers = true or r.userId in :userIds)
          and (:hiddenOnly = false or (r.deletedAt is not null and r.deletedByAdmin = true))
    """)
    Page<Review> findForAdmin(@Param("allProducts") boolean allProducts,
                              @Param("productIds") Collection<Long> productIds,
                              @Param("allUsers") boolean allUsers,
                              @Param("userIds") Collection<Long> userIds,
                              @Param("hiddenOnly") boolean hiddenOnly,
                              Pageable pageable);
}
