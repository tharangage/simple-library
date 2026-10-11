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
        int borrowedCount,
        int maxItems,
        int remainingSlots) {

    /** A borrower holds a handful of books, so the count always fits an int. */
    public static BorrowerProfile of(final Borrower borrower, final long borrowedCount, final int maxItems) {
        final int borrowed = Math.toIntExact(borrowedCount);
        return new BorrowerProfile(
                borrower.getId(),
                borrower.getMobile(),
                borrowed,
                maxItems,
                Math.max(0, maxItems - borrowed));
    }
}
