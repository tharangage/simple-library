package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Borrower;

/**
 * What only a library member has, as the {@code borrower} section of {@link UserProfileResponse}: the Borrower
 * id (used in the borrow path) and how many books they hold ("x of 3 used").
 * Name, email and mobile are not repeated here: they are at the top level of {@link UserProfileResponse}.
 */
public record BorrowerProfile(
        String id,
        int borrowedCount,
        int maxItems,
        int remainingSlots) {

    /** A borrower holds a handful of books, so the count always fits an int. */
    public static BorrowerProfile of(final Borrower borrower, final long borrowedCount, final int maxItems) {
        final int borrowed = Math.toIntExact(borrowedCount);
        return new BorrowerProfile(
                borrower.getId(),
                borrowed,
                maxItems,
                Math.max(0, maxItems - borrowed));
    }
}
