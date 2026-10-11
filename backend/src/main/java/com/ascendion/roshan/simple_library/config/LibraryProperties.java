package com.ascendion.roshan.simple_library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Library rules that can change without a code change.
 *
 * @param maxItemsPerBorrower most items a borrower may hold at once (loans today, loans + reservations with E05)
 */
@ConfigurationProperties(prefix = "app.library")
public record LibraryProperties(@DefaultValue("3") int maxItemsPerBorrower) {
}
