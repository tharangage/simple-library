package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Borrower;

import java.time.LocalDateTime;

/**
 * The signed-in user's own Borrower plus how many items they hold ("x of 3 used" in the web app).
 * Only returned to the owner, so the mobile number is included.
 */
public record MyBorrowerResponse(
        String id,
        String firstname,
        String lastname,
        String email,
        String mobile,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate,
        long borrowedCount,
        int maxItems,
        long remainingSlots) {

    public static MyBorrowerResponse of(final Borrower borrower, final long borrowedCount, final int maxItems) {
        return new MyBorrowerResponse(
                borrower.getId(),
                borrower.getFirstname(),
                borrower.getLastname(),
                borrower.getEmail(),
                borrower.getMobile(),
                borrower.getCreatedDate(),
                borrower.getLastModifiedDate(),
                borrowedCount,
                maxItems,
                Math.max(0, maxItems - borrowedCount));
    }
}
