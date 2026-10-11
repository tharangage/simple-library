package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Borrower;

/**
 * What only a library member has, as the {@code borrower} section of {@link UserProfileResponse}: the Borrower
 * id (used in the borrow path), the mobile number and how many books they hold ("x of 3 used").
 * The name and email are not repeated here: {@link UserProfileResponse} already carries them.
 * Only returned to the owner, so the mobile number is included.
 */
public record BorrowerProfile(
        String id,
        String mobile,
        long borrowedCount,
        int maxItems,
        long remainingSlots) {

    public static BorrowerProfile of(final Borrower borrower, final long borrowedCount, final int maxItems) {
        return new BorrowerProfile(
                borrower.getId(),
                borrower.getMobile(),
                borrowedCount,
                maxItems,
                Math.max(0, maxItems - borrowedCount));
    }
}
