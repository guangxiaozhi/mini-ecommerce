package com.guang.miniecommercebackend.entity;

public enum OrderStatus {
    PENDING,     // order placed, inventory allocated
    PAID,        // payment received
    PROCESSING,  // being picked/packed
    SHIPPED,     // dispatched — inventory fulfilled (on_hand reduced)
    DELIVERED,   // customer received
    CLOSED,      // finalized, no more returns
    CANCELLED    // cancelled — allocated inventory released
}
