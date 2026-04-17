package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ReturnRequest;
import com.guang.miniecommercebackend.entity.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);
    List<ReturnRequest> findByOrderId(Long orderId);
    List<ReturnRequest> findByRequestedAtBetween(LocalDateTime from, LocalDateTime to);

    // Check if an active (non-rejected) return already exists for an order
    List<ReturnRequest> findByOrderIdAndStatusNot(Long orderId, ReturnStatus status);
}
