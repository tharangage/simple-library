package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Creates the Borrower on a user's first authenticated call (ADR-001, option 2a), for users whose
 * Keycloak account came from the Keycloak console or a brokered identity provider.
 * Linked by the token's {@code sub}. Not {@code @Transactional}: the insert runs in its own
 * transaction ({@link BorrowerInserter}) so a duplicate-key race can be recovered by re-reading.
 */
@Service
@Slf4j
public class BorrowerProvisioningService {

    private final BorrowerRepository borrowerRepository;
    private final BorrowerInserter inserter;

    public BorrowerProvisioningService(final BorrowerRepository borrowerRepository, final BorrowerInserter inserter) {
        this.borrowerRepository = borrowerRepository;
        this.inserter = inserter;
    }

    /** Callers are limited to role {@code user} by {@code BorrowerProvisioningInterceptor} and {@code SecurityConfig}. */
    public Borrower getOrCreate(final Jwt jwt) {
        final String sub = jwt.getSubject();
        final Optional<Borrower> existing = borrowerRepository.findByKeycloakUserId(sub);
        if (existing.isPresent()) {
            return existing.get();   // the stored profile is never overwritten by later tokens
        }

        final String email = normalizedEmail(jwt);
        if (email != null) {
            final Optional<Borrower> sameEmail = borrowerRepository.findFirstByEmailIgnoreCase(email);
            if (sameEmail.isPresent()) {
                return linkLegacyBorrower(sameEmail.get(), sub, jwt);
            }
        }

        final String firstname = firstNonBlank(jwt.getClaimAsString("given_name"), jwt.getClaimAsString("preferred_username"));
        final String lastname = firstNonBlank(jwt.getClaimAsString("family_name"), jwt.getClaimAsString("preferred_username"));
        if (firstname == null && lastname == null && email == null) {
            throw new IllegalStateException("Token has no name or email to create a borrower from");
        }

        final Borrower borrower = new Borrower();
        borrower.setFirstname(firstname);
        borrower.setLastname(lastname);
        // An unverified email is not stored: it could belong to someone else and would block them via the unique index.
        borrower.setEmail(Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")) ? email : null);
        borrower.setKeycloakUserId(sub);
        try {
            return inserter.insert(borrower);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent first call inserted the same keycloakUserId: return that row.
            // Otherwise the email unique index fired: another account owns that email.
            return borrowerRepository.findByKeycloakUserId(sub)
                    .orElseThrow(() -> new IllegalStateException("Email already registered to another account", ex));
        }
    }

    /** An older row with this email and no keycloakUserId is linked only for a verified email. */
    private Borrower linkLegacyBorrower(final Borrower legacy, final String sub, final Jwt jwt) {
        if (legacy.getKeycloakUserId() != null) {
            throw new IllegalStateException("Email already registered to another account");
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new IllegalStateException("Email matches an existing borrower but is not verified");
        }
        // One conditional UPDATE: if another request linked the row first, 0 rows change and nothing is overwritten.
        final int linked = borrowerRepository.linkIfUnlinked(legacy.getId(), sub);
        if (linked > 0) {
            log.info("Linked existing borrower {} to Keycloak user {}", legacy.getId(), sub.replaceAll("[^A-Za-z0-9-]", "_"));
        }
        // Re-read: returns our row if we (or a concurrent call for the same user) linked it, else the row belongs to someone else.
        return borrowerRepository.findByKeycloakUserId(sub)
                .orElseThrow(() -> new IllegalStateException("Email already registered to another account"));
    }

    private static String normalizedEmail(final Jwt jwt) {
        final String email = jwt.getClaimAsString("email");
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private static String firstNonBlank(final String preferred, final String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : (StringUtils.hasText(fallback) ? fallback.trim() : null);
    }
}
