package com.ascendion.roshan.simple_library.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Library rules that can change without a code change.
 *
 * @param maxItemsPerBorrower most items a borrower may hold at once (loans today, loans + reservations with E05);
 *                            at least 1, otherwise nobody could borrow and the application refuses to start
 */
@Validated
@ConfigurationProperties(prefix = "app.library")
public record LibraryProperties(@DefaultValue("3") @Min(1) int maxItemsPerBorrower) {
}
