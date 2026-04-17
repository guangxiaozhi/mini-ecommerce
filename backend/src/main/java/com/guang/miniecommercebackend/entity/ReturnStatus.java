package com.guang.miniecommercebackend.entity;

public enum ReturnStatus {
    REQUESTED,  // customer submitted return request
    APPROVED,   // admin approved — inventory restocked
    REFUNDED,   // refund confirmed
    REJECTED    // admin rejected
}
