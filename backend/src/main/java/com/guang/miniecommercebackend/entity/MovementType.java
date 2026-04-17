package com.guang.miniecommercebackend.entity;

public enum MovementType {
    RECEIVE,      // new stock arrives → on_hand+, available+
    ALLOCATE,     // order placed     → allocated+, available-
    DEALLOCATE,   // order cancelled  → allocated-, available+
    FULFILL,      // order shipped    → on_hand-, allocated-
    RETURN,       // return approved  → on_hand+, available+
    ADJUST        // manual correction → on_hand±, available±
}
