package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {
    List<ReturnItem> findByReturnRequestId(Long returnRequestId);
}
