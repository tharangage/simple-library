package com.ascendion.roshan.simple_library.service;

import java.util.Locale;

/** Builds the LIKE pattern for the {@code q} search text of the list endpoints. */
final class SearchPattern {

    private SearchPattern() {
    }

    /** The search text without surrounding blanks, lower case. Empty when there is nothing to search for. */
    static String normalize(final String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Matches the text anywhere (case-insensitive when the column is lower-cased in the query). Wraps it in % and
     * escapes the LIKE wildcards in it (escape character !), so "50%" is searched as text.
     */
    static String contains(final String query) {
        final String escaped = normalize(query).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
