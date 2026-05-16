package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(@Min(1) @Max(5) Integer rating,
                                  @Size(min = 10, max = 1000) String comment) {
    @AssertTrue(message = "At least one of rating or comment must be provided")
    public  boolean isAtLeastOneFieldPresent(){
        return rating != null || comment != null;
    }
}
