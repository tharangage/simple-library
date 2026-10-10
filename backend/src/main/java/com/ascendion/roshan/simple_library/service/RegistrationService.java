package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.RegistrationRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.EmailAlreadyRegisteredException;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Self-registration: create the Keycloak user, then the Borrower that points to it.
 * Not {@code @Transactional}: Keycloak cannot join a database transaction, so a failed save
 * is undone by deleting the Keycloak user again (compensation).
 */
@Service
@Slf4j
public class RegistrationService {

    private final KeycloakAdminClient keycloak;
    private final BorrowerRepository borrowerRepository;

    public RegistrationService(final KeycloakAdminClient keycloak, final BorrowerRepository borrowerRepository) {
        this.keycloak = keycloak;
        this.borrowerRepository = borrowerRepository;
    }

    public Borrower register(final RegistrationRequest request) {
        final String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (borrowerRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        final String keycloakUserId = keycloak.createUser(
                email, request.firstName().trim(), request.lastName().trim(), request.mobile(), request.password());

        final Borrower borrower = new Borrower();
        borrower.setFirstname(request.firstName().trim());
        borrower.setLastname(request.lastName().trim());
        borrower.setEmail(email);
        borrower.setMobile(request.mobile());
        borrower.setKeycloakUserId(keycloakUserId);
        try {
            return borrowerRepository.saveAndFlush(borrower);
        } catch (DataIntegrityViolationException ex) {
            // Someone registered the same email between our check and the insert.
            compensate(keycloakUserId);
            throw new EmailAlreadyRegisteredException(ex);
        } catch (RuntimeException ex) {
            compensate(keycloakUserId);
            throw ex;   // answered as 500
        }
    }

    private void compensate(final String keycloakUserId) {
        try {
            keycloak.deleteUser(keycloakUserId);
        } catch (RuntimeException deleteFailure) {
            // Needs manual cleanup. Log the Keycloak id only, never the email, mobile or password.
            log.error("Could not delete Keycloak user {} after a failed registration", keycloakUserId, deleteFailure);
        }
    }
}
