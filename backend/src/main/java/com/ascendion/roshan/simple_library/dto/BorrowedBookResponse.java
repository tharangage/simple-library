package com.ascendion.roshan.simple_library.dto;

import java.time.LocalDateTime;

/**
 * One borrowed book, for the admin returns screen. Shows the borrower's name and email, so the
 * endpoint that returns it is admin-only.
 */
public record BorrowedBookResponse(
        String bookId,
        String title,
        String author,
        String isbn,
        LocalDateTime borrowedDate,
        String borrowerId,
        String borrowerFirstname,
        String borrowerLastname,
        String borrowerEmail) {
}
